package com.retrocrawler.core.progress;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Tracks an operation and can divide its range into nested, weighted
 * sub-progressors.
 *
 * <p>
 * A sub-progressor owns a window in its root progressor. Advancing it updates
 * the root fraction while snapshots retain the sub-progressor's current stage
 * work units and accuracy.
 *
 * <p>
 * Adapted from progressor code developed by Relimit GmbH. Used in
 * RetroCrawler with permission.
 */
public final class Progressor {

	private static final AtomicLong IDS = new AtomicLong();

	private final Root root;

	private final double offset;

	private final double window;

	private final boolean rootProgressor;

	private ProgressStage stage = ProgressStage.IDLE;

	private String message = "";

	private long completed = -1;

	private long total = -1;

	private ProgressAccuracy accuracy = ProgressAccuracy.INDETERMINATE;

	private long startedNanos = System.nanoTime();

	public Progressor() {
		this.root = new Root("progress-" + IDS.incrementAndGet());
		this.offset = 0;
		this.window = 1;
		this.rootProgressor = true;
	}

	private Progressor(final Root root, final double offset, final double window) {
		this.root = root;
		this.offset = offset;
		this.window = window;
		this.rootProgressor = false;
	}

	public static Progressor observing(final ProgressMonitor monitor) {
		return new Progressor().withMonitor(monitor);
	}

	public static Progressor reportingMessages(final Consumer<String> messageConsumer) {
		Objects.requireNonNull(messageConsumer, "messageConsumer");
		return observing(progress -> messageConsumer.accept(progress.message()));
	}

	public Progressor withMonitor(final ProgressMonitor monitor) {
		root.monitors.add(Objects.requireNonNull(monitor, "monitor"));
		return this;
	}

	public Progressor begin(final ProgressStage newStage, final String newMessage, final long newTotal,
			final ProgressAccuracy newAccuracy) {
		Objects.requireNonNull(newStage, "newStage");
		Objects.requireNonNull(newMessage, "newMessage");
		Objects.requireNonNull(newAccuracy, "newAccuracy");
		if (newTotal < 0) {
			throw new IllegalArgumentException("Total work units must not be negative.");
		}
		if (newAccuracy == ProgressAccuracy.INDETERMINATE) {
			throw new IllegalArgumentException("Determinate progress requires exact or approximate accuracy.");
		}
		throwIfCancelled();

		synchronized (root) {
			stage = newStage;
			message = newMessage;
			completed = 0;
			total = newTotal;
			accuracy = newAccuracy;
			startedNanos = System.nanoTime();
			root.activeStageStartedNanos = startedNanos;
		}
		publish(ProgressState.RUNNING, offset);
		return this;
	}

	public Progressor indeterminate(final ProgressStage newStage, final String newMessage) {
		Objects.requireNonNull(newStage, "newStage");
		Objects.requireNonNull(newMessage, "newMessage");
		throwIfCancelled();

		synchronized (root) {
			stage = newStage;
			message = newMessage;
			completed = -1;
			total = -1;
			accuracy = ProgressAccuracy.INDETERMINATE;
			startedNanos = System.nanoTime();
			root.activeStageStartedNanos = startedNanos;
		}
		publish(ProgressState.RUNNING, offset);
		return this;
	}

	public Progressor advance() {
		return advanceBy(1);
	}

	public Progressor advanceBy(final long delta) {
		final String currentMessage;
		synchronized (root) {
			currentMessage = message;
		}
		return advanceBy(delta, currentMessage);
	}

	public Progressor advanceBy(final long delta, final String newMessage) {
		if (delta < 0) {
			throw new IllegalArgumentException("Progress delta must not be negative.");
		}
		final long target;
		synchronized (root) {
			target = completed + delta;
		}
		return advanceTo(target, newMessage);
	}

	public Progressor advanceTo(final long target, final String newMessage) {
		Objects.requireNonNull(newMessage, "newMessage");
		throwIfCancelled();

		final double localFraction;
		synchronized (root) {
			if (accuracy == ProgressAccuracy.INDETERMINATE) {
				throw new IllegalStateException("Begin determinate progress before advancing.");
			}
			completed = Math.max(0, Math.min(target, total));
			message = newMessage;
			localFraction = total == 0 ? 0 : (double) completed / total;
		}
		publish(ProgressState.RUNNING, offset + window * localFraction);
		return this;
	}

	public Progressor advanceTo(final long target) {
		final String currentMessage;
		synchronized (root) {
			currentMessage = message;
		}
		return advanceTo(target, currentMessage);
	}

	public Progressor advanceToEnd() {
		final String currentMessage;
		synchronized (root) {
			currentMessage = message;
		}
		return advanceToEnd(currentMessage);
	}

	public Progressor advanceToEnd(final String newMessage) {
		final long target;
		synchronized (root) {
			if (accuracy == ProgressAccuracy.INDETERMINATE) {
				throw new IllegalStateException("Begin determinate progress before advancing.");
			}
			target = total;
		}
		return advanceTo(target, newMessage);
	}

	public Progressor[] splitIntoEqualParts(final int parts) {
		if (parts <= 0) {
			throw new IllegalArgumentException("Require at least one progress part.");
		}
		final double[] weights = new double[parts];
		java.util.Arrays.fill(weights, 1);
		return splitInRelationTo(weights);
	}

	public Progressor[] splitInRelationTo(final double... weights) {
		Objects.requireNonNull(weights, "weights");
		if (weights.length == 0) {
			throw new IllegalArgumentException("Require at least one progress weight.");
		}

		double totalWeight = 0;
		for (final double weight : weights) {
			if (!Double.isFinite(weight) || weight < 0) {
				throw new IllegalArgumentException("Progress weights must be finite and non-negative.");
			}
			totalWeight += weight;
		}
		if (!Double.isFinite(totalWeight) || totalWeight <= 0) {
			throw new IllegalArgumentException("The sum of progress weights must be finite and positive.");
		}

		final Progressor[] result = new Progressor[weights.length];
		double localOffset = 0;
		for (int index = 0; index < weights.length; index++) {
			final double share = weights[index] / totalWeight;
			final double childOffset = offset + window * localOffset;
			final double childWindow = index == weights.length - 1
					? offset + window - childOffset
					: window * share;
			result[index] = new Progressor(root, childOffset, childWindow);
			localOffset += share;
		}
		return result;
	}

	public double progress() {
		synchronized (root) {
			if (window == 0) {
				return 0;
			}
			return clamp((root.fraction - offset) / window);
		}
	}

	public ProgressSnapshot snapshot() {
		synchronized (root) {
			return root.snapshot;
		}
	}

	public boolean isCancelled() {
		return root.cancelled.get();
	}

	public void throwIfCancelled() {
		if (isCancelled()) {
			throw new ProgressCancelledException("Operation was cancelled.");
		}
	}

	public void cancel(final String cancellationMessage) {
		Objects.requireNonNull(cancellationMessage, "cancellationMessage");
		if (!root.cancelled.compareAndSet(false, true)) {
			return;
		}
		publishTerminal(ProgressState.CANCELLED, cancellationMessage, currentRootFraction(), false);
	}

	public void complete(final String completionMessage) {
		Objects.requireNonNull(completionMessage, "completionMessage");
		if (isCancelled()) {
			return;
		}
		if (rootProgressor) {
			publishTerminal(ProgressState.COMPLETE, completionMessage, 1, true);
			return;
		}
		synchronized (root) {
			message = completionMessage;
			if (accuracy != ProgressAccuracy.INDETERMINATE) {
				completed = total;
			}
		}
		publish(ProgressState.RUNNING, offset + window);
	}

	public void fail(final String failureMessage) {
		Objects.requireNonNull(failureMessage, "failureMessage");
		if (isCancelled()) {
			return;
		}
		publishTerminal(ProgressState.FAILED, failureMessage, currentRootFraction(), false);
	}

	private double currentRootFraction() {
		synchronized (root) {
			return root.fraction;
		}
	}

	private void publish(final ProgressState state, final double rootFraction) {
		final ProgressSnapshot next;
		synchronized (root) {
			if (state == ProgressState.RUNNING && root.cancelled.get()) {
				return;
			}
			root.fraction = clamp(rootFraction);
			final Duration elapsed = Duration.ofNanos(Math.max(0, System.nanoTime() - startedNanos));
			final Optional<Duration> remaining = estimateRemaining(elapsed);
			next = new ProgressSnapshot(root.id, stage, message, completed, total, accuracy, state,
					root.fraction, elapsed, remaining);
			root.snapshot = next;
		}
		notifyMonitors(next);
	}

	private void publishTerminal(final ProgressState state, final String terminalMessage, final double rootFraction,
			final boolean finishUnits) {
		final ProgressSnapshot next;
		synchronized (root) {
			if (state != ProgressState.CANCELLED && root.cancelled.get()) {
				return;
			}
			final ProgressSnapshot current = root.snapshot;
			final long terminalCompleted = finishUnits && current.isDeterminate()
					? current.total()
					: current.completed();
			final Duration elapsed = Duration.ofNanos(
					Math.max(0, System.nanoTime() - root.activeStageStartedNanos));
			root.fraction = clamp(rootFraction);
			next = new ProgressSnapshot(root.id, current.stage(), terminalMessage, terminalCompleted, current.total(),
					current.accuracy(), state, root.fraction, elapsed, Optional.empty());
			root.snapshot = next;
		}
		notifyMonitors(next);
	}

	private void notifyMonitors(final ProgressSnapshot progress) {
		synchronized (root.notificationLock) {
			for (final ProgressMonitor monitor : root.monitors) {
				synchronized (root) {
					if (root.snapshot != progress) {
						return;
					}
				}
				monitor.onProgress(progress);
			}
		}
	}

	private Optional<Duration> estimateRemaining(final Duration elapsed) {
		if (accuracy == ProgressAccuracy.INDETERMINATE || total <= 0 || completed <= 0 || completed >= total) {
			return Optional.empty();
		}
		final double fraction = (double) completed / total;
		final double remainingNanos = elapsed.toNanos() * ((1 - fraction) / fraction);
		if (!Double.isFinite(remainingNanos) || remainingNanos < 0 || remainingNanos > Long.MAX_VALUE) {
			return Optional.empty();
		}
		return Optional.of(Duration.ofNanos(Math.round(remainingNanos)));
	}

	private static double clamp(final double value) {
		return Math.max(0, Math.min(1, value));
	}

	private static final class Root {

		private final String id;

		private final CopyOnWriteArrayList<ProgressMonitor> monitors = new CopyOnWriteArrayList<>();

		private final AtomicBoolean cancelled = new AtomicBoolean();

		private final Object notificationLock = new Object();

		private double fraction;

		private long activeStageStartedNanos = System.nanoTime();

		private ProgressSnapshot snapshot;

		private Root(final String id) {
			this.id = id;
			this.snapshot = new ProgressSnapshot(id, ProgressStage.IDLE, "", -1, -1,
					ProgressAccuracy.INDETERMINATE, ProgressState.RUNNING, 0, Duration.ZERO, Optional.empty());
		}
	}
}
