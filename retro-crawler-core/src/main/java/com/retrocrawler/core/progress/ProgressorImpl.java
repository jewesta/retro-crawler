package com.retrocrawler.core.progress;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleSupplier;

/** Default, dependency-free {@link Progressor} implementation. */
public class ProgressorImpl extends AbstractProgressor {

	private static final int DEFAULT_QUEUE_SIZE = 2_000;

	private static final AtomicLong IDS = new AtomicLong();

	private final String id;

	private final Deque<ProgressToken> progressTokens = new ArrayDeque<>(DEFAULT_QUEUE_SIZE);

	private final CopyOnWriteArrayList<ProgressMonitor> monitors = new CopyOnWriteArrayList<>();

	private final AtomicBoolean cancellation = new AtomicBoolean();

	private final Object notificationLock = new Object();

	private double rootProgress;

	private ProgressState currentState = ProgressState.RUNNING;

	private ProgressSnapshot currentSnapshot;

	public ProgressorImpl() {
		this("progress-" + IDS.incrementAndGet());
	}

	protected ProgressorImpl(final String id) {
		this.id = id;
		final long now = System.nanoTime();
		currentStartedNanos = now;
		progressTokens.addLast(new ProgressToken(now, 0));
		currentSnapshot = new ProgressSnapshot(id, currentStage, currentMessage, currentCompleted, currentTotal,
				currentAccuracy, currentState, rootProgress, rootProgress, Duration.ZERO, Optional.empty());
	}

	@Override
	public String id() {
		return id;
	}

	@Override
	public synchronized ProgressSnapshot snapshot() {
		return currentSnapshot;
	}

	synchronized double rootProgress() {
		return rootProgress;
	}

	synchronized ProgressState rootState() {
		return currentState;
	}

	boolean cancelled() {
		return cancellation.get();
	}

	void resetFrom(final AbstractProgressor source, final double target) {
		final ProgressSnapshot snapshot;
		synchronized (this) {
			if (source == this) {
				progressTokens.clear();
			}
			currentState = ProgressState.RUNNING;
			snapshot = update(source, target, true);
		}
		notifyMonitors(snapshot);
	}

	void indeterminateFrom(final AbstractProgressor source, final double target) {
		final ProgressSnapshot snapshot;
		synchronized (this) {
			if (source == this) {
				progressTokens.clear();
			}
			currentState = ProgressState.RUNNING;
			snapshot = update(source, target, true);
		}
		notifyMonitors(snapshot);
	}

	void advanceFrom(final AbstractProgressor source, final double offset, final double window,
			final DoubleSupplier targetSupplier) {
		final ProgressSnapshot snapshot;
		synchronized (this) {
			if (cancellation.get()) {
				throw new ProgressCancelledException("Operation was cancelled.");
			}
			final double normalized = respectBounds(targetSupplier.getAsDouble());
			if (source.currentAccuracy != ProgressAccuracy.INDETERMINATE) {
				source.currentCompleted = Math.round(normalized * source.currentTotal);
			}
			currentState = ProgressState.RUNNING;
			snapshot = update(source, offset + window * normalized, true);
		}
		notifyMonitors(snapshot);
	}

	void metadataFrom(final AbstractProgressor source) {
		final ProgressSnapshot snapshot;
		synchronized (this) {
			adopt(source);
			snapshot = freeze();
		}
		notifyMonitors(snapshot);
	}

	void completeFrom(final AbstractProgressor source) {
		final ProgressSnapshot snapshot;
		synchronized (this) {
			adopt(source);
			rootProgress = 1;
			currentState = ProgressState.COMPLETE;
			addToken(new ProgressToken(System.nanoTime(), rootProgress));
			snapshot = freezeWithoutRemaining();
		}
		notifyMonitors(snapshot);
	}

	void failFrom(final AbstractProgressor source, final String message) {
		final ProgressSnapshot snapshot;
		synchronized (this) {
			adopt(source);
			currentMessage = message;
			currentState = ProgressState.FAILED;
			snapshot = freezeWithoutRemaining();
		}
		notifyMonitors(snapshot);
	}

	void cancelFrom(final AbstractProgressor source, final String message) {
		if (!cancellation.compareAndSet(false, true)) {
			return;
		}
		final ProgressSnapshot snapshot;
		synchronized (this) {
			adopt(source);
			currentMessage = message;
			currentState = ProgressState.CANCELLED;
			snapshot = freezeWithoutRemaining();
		}
		notifyMonitors(snapshot);
	}

	void addMonitor(final ProgressMonitor monitor) {
		monitors.add(monitor);
	}

	synchronized Duration elapsedFor(final AbstractProgressor source) {
		final ProgressToken latest = progressTokens.getLast();
		return Duration.ofNanos(Math.max(0, latest.nanos() - source.currentStartedNanos));
	}

	synchronized Optional<Duration> remainingTime() {
		if (currentAccuracy == ProgressAccuracy.INDETERMINATE || rootProgress >= 1 || progressTokens.size() < 2) {
			return Optional.empty();
		}
		final ProgressToken oldest = progressTokens.getFirst();
		final ProgressToken latest = progressTokens.getLast();
		final double advanced = latest.progress() - oldest.progress();
		final long elapsedNanos = latest.nanos() - oldest.nanos();
		if (advanced <= 0 || elapsedNanos <= 0) {
			return Optional.empty();
		}
		final double nanos = (1 - rootProgress) / (advanced / elapsedNanos);
		if (!Double.isFinite(nanos) || nanos < 0 || nanos > Long.MAX_VALUE) {
			return Optional.empty();
		}
		return Optional.of(Duration.ofNanos(Math.round(nanos)));
	}

	private ProgressSnapshot update(final AbstractProgressor source, final double target, final boolean token) {
		adopt(source);
		rootProgress = respectBounds(target);
		if (token) {
			addToken(new ProgressToken(System.nanoTime(), rootProgress));
		}
		return freeze();
	}

	private void adopt(final AbstractProgressor source) {
		if (source == this) {
			return;
		}
		currentStage = source.currentStage;
		currentMessage = source.currentMessage;
		currentCompleted = source.currentCompleted;
		currentTotal = source.currentTotal;
		currentAccuracy = source.currentAccuracy;
		currentStartedNanos = source.currentStartedNanos;
	}

	private void addToken(final ProgressToken token) {
		if (progressTokens.size() == DEFAULT_QUEUE_SIZE) {
			progressTokens.removeFirst();
		}
		progressTokens.addLast(token);
	}

	private ProgressSnapshot freeze() {
		currentSnapshot = new ProgressSnapshot(id, currentStage, currentMessage, currentCompleted, currentTotal,
				currentAccuracy, currentState, rootProgress, rootProgress, elapsedFor(this), remainingTime());
		return currentSnapshot;
	}

	private ProgressSnapshot freezeWithoutRemaining() {
		currentSnapshot = new ProgressSnapshot(id, currentStage, currentMessage, currentCompleted, currentTotal,
				currentAccuracy, currentState, rootProgress, rootProgress, elapsedFor(this), Optional.empty());
		return currentSnapshot;
	}

	private void notifyMonitors(final ProgressSnapshot snapshot) {
		synchronized (notificationLock) {
			for (final ProgressMonitor monitor : monitors) {
				synchronized (this) {
					if (currentSnapshot != snapshot) {
						return;
					}
				}
				monitor.monitorProgress(snapshot);
			}
		}
	}

	private record ProgressToken(long nanos, double progress) {
	}
}
