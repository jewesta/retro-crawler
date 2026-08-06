package com.retrocrawler.core.progress;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.DoubleSupplier;

/**
 * Common progress and splitting behavior for root and sub-progressors.
 *
 * <p>
 * A sub-progressor owns a window of its parent. It keeps no separate cursor:
 * advancing it moves the root cursor inside that window. Sub-progressors can be
 * split again without changing that model.
 */
public abstract class AbstractProgressor implements Progressor {

	protected static final long DEFAULT_GRANULARITY = 100_000;

	private static final BigDecimal ONE = BigDecimal.ONE;

	protected long maximum = DEFAULT_GRANULARITY;

	protected ProgressStage currentStage = ProgressStage.IDLE;

	protected String currentMessage = "";

	protected long currentCompleted;

	protected long currentTotal = DEFAULT_GRANULARITY;

	protected ProgressAccuracy currentAccuracy = ProgressAccuracy.EXACT;

	protected long currentStartedNanos = System.nanoTime();

	private final ProgressorImpl root;

	private final double rootOffset;

	private final double rootWindow;

	protected AbstractProgressor() {
		root = null;
		rootOffset = 0;
		rootWindow = 1;
	}

	private AbstractProgressor(final ProgressorImpl root, final double rootOffset, final double rootWindow) {
		this.root = root;
		this.rootOffset = rootOffset;
		this.rootWindow = rootWindow;
	}

	@Override
	public Progressor reset() {
		return reset(maximum);
	}

	@Override
	public Progressor reset(final long newMaximum) {
		if (newMaximum < 0) {
			throw new IllegalArgumentException("Maximum progress must not be negative.");
		}
		throwIfCancelled();
		maximum = newMaximum;
		currentCompleted = 0;
		currentTotal = newMaximum;
		currentAccuracy = ProgressAccuracy.EXACT;
		currentStartedNanos = System.nanoTime();
		root().resetFrom(this, rootOffset);
		return this;
	}

	@Override
	public Progressor advanceBy(final double delta) {
		return advanceToNormalized(() -> progress() + delta / maximum);
	}

	@Override
	public Progressor advanceBy(final double delta, final String message) {
		currentMessage = Objects.requireNonNull(message, "message");
		return advanceBy(delta);
	}

	@Override
	public Progressor advanceTo(final double position) {
		return advanceToBase(position, maximum);
	}

	@Override
	public Progressor advanceTo(final double position, final String message) {
		currentMessage = Objects.requireNonNull(message, "message");
		return advanceTo(position);
	}

	@Override
	public Progressor advanceToBase(final double position, final double base) {
		return advanceToNormalized(() -> base <= 0 ? 0 : position / base);
	}

	@Override
	public Progressor advanceToBase1(final double position) {
		return advanceToNormalized(() -> position);
	}

	@Override
	public Progressor advanceToBase100(final double percentage) {
		return advanceToBase(percentage, 100);
	}

	@Override
	public Progressor advanceToEnd() {
		return advanceTo(maximum);
	}

	@Override
	public Progressor advanceToEnd(final String message) {
		return advanceTo(maximum, message);
	}

	@Override
	public Progressor begin(final ProgressStage stage, final String message, final long total,
			final ProgressAccuracy accuracy) {
		if (total < 0) {
			throw new IllegalArgumentException("Total work units must not be negative.");
		}
		throwIfCancelled();
		currentStage = Objects.requireNonNull(stage, "stage");
		currentMessage = Objects.requireNonNull(message, "message");
		currentAccuracy = Objects.requireNonNull(accuracy, "accuracy");
		if (accuracy == ProgressAccuracy.INDETERMINATE) {
			throw new IllegalArgumentException("Determinate progress requires exact or approximate accuracy.");
		}
		maximum = total;
		currentCompleted = 0;
		currentTotal = total;
		currentStartedNanos = System.nanoTime();
		root().resetFrom(this, rootOffset);
		return this;
	}

	@Override
	public Progressor indeterminate(final ProgressStage stage, final String message) {
		throwIfCancelled();
		currentStage = Objects.requireNonNull(stage, "stage");
		currentMessage = Objects.requireNonNull(message, "message");
		currentCompleted = -1;
		currentTotal = -1;
		currentAccuracy = ProgressAccuracy.INDETERMINATE;
		currentStartedNanos = System.nanoTime();
		root().indeterminateFrom(this, rootOffset);
		return this;
	}

	@Override
	public Progressor[] splitByPercentages(final BigDecimal... percentages) {
		Objects.requireNonNull(percentages, "percentages");
		final Progressor[] children = new Progressor[percentages.length];
		BigDecimal consumed = BigDecimal.ZERO;
		for (int index = 0; index < percentages.length; index++) {
			final BigDecimal percentage = Objects.requireNonNull(percentages[index], "percentage");
			if (percentage.signum() < 0) {
				throw new IllegalArgumentException("Progress percentages must not be negative.");
			}
			final BigDecimal next = consumed.add(percentage);
			if (next.compareTo(ONE) > 0) {
				throw new IllegalArgumentException("Total percentage exceeds 100%.");
			}
			final double childOffset = rootOffset + rootWindow * consumed.doubleValue();
			final double childWindow = rootWindow * percentage.doubleValue();
			children[index] = new SubProgressor(root(), id(), index, childOffset, childWindow);
			consumed = next;
		}
		return children;
	}

	@Override
	public Progressor[] splitByPercentages(final Collection<BigDecimal> percentages) {
		Objects.requireNonNull(percentages, "percentages");
		return splitByPercentages(percentages.toArray(BigDecimal[]::new));
	}

	@Override
	public Progressor[] splitIntoEqualParts(final int parts) {
		if (parts <= 0) {
			throw new IllegalArgumentException("Require at least one progress part.");
		}
		final double[] segments = new double[parts];
		Arrays.fill(segments, 1);
		return splitByPercentages(ProgressUtils.toPercentages(10, segments));
	}

	@Override
	public Progressor[] splitInRelationTo(final double... segments) {
		Objects.requireNonNull(segments, "segments");
		return splitByPercentages(ProgressUtils.toPercentages(10, segments));
	}

	@Override
	public Progressor[] splitInRelationTo(final Collection<Double> segments) {
		Objects.requireNonNull(segments, "segments");
		final double[] values = new double[segments.size()];
		int index = 0;
		for (final Double segment : segments) {
			values[index++] = Objects.requireNonNull(segment, "segment");
		}
		return splitInRelationTo(values);
	}

	@Override
	public Progressor setStepLabel(final String label) {
		throwIfCancelled();
		currentMessage = Objects.requireNonNull(label, "label");
		root().metadataFrom(this);
		return this;
	}

	@Override
	public Progressor withMonitor(final ProgressMonitor monitor) {
		root().addMonitor(Objects.requireNonNull(monitor, "monitor"));
		return this;
	}

	@Override
	public AutoProgressor autoProgress(final long millis) {
		if (millis < 0) {
			throw new IllegalArgumentException("Automatic progress duration must not be negative.");
		}
		final Progressor controlled = this;
		return new AutoProgressor() {

			private long updateIntervalMillis = 1_000;

			private Timer timer;

			private long startedNanos;

			@Override
			public synchronized void run() {
				if (timer != null) {
					throw new IllegalStateException("Automatic progress is already running.");
				}
				controlled.reset(millis);
				startedNanos = System.nanoTime();
				timer = new Timer("progressor-" + id(), true);
				timer.schedule(new TimerTask() {

					@Override
					public void run() {
						final long elapsed = (System.nanoTime() - startedNanos) / 1_000_000;
						if (elapsed >= millis) {
							done();
						} else {
							controlled.advanceTo(elapsed);
						}
					}
				}, updateIntervalMillis, updateIntervalMillis);
			}

			@Override
			public synchronized void done() {
				if (timer != null) {
					timer.cancel();
					timer = null;
				}
				controlled.advanceToEnd();
			}

			@Override
			public synchronized AutoProgressor updateInterval(final long intervalMillis) {
				if (timer != null) {
					throw new IllegalStateException(
							"Cannot change the update interval while automatic progress is running.");
				}
				if (intervalMillis <= 0) {
					throw new IllegalArgumentException("Update interval must be positive.");
				}
				updateIntervalMillis = intervalMillis;
				return this;
			}
		};
	}

	@Override
	public double progress() {
		if (rootWindow == 0) {
			return 0;
		}
		return respectBounds((root().rootProgress() - rootOffset) / rootWindow);
	}

	@Override
	public double overallFraction() {
		return root().rootProgress();
	}

	@Override
	public ProgressStage stage() {
		return currentStage;
	}

	@Override
	public String message() {
		return currentMessage;
	}

	@Override
	public long completed() {
		return currentCompleted;
	}

	@Override
	public long total() {
		return currentTotal;
	}

	@Override
	public ProgressAccuracy accuracy() {
		return currentAccuracy;
	}

	@Override
	public ProgressState state() {
		return root().rootState();
	}

	@Override
	public Duration elapsed() {
		return root().elapsedFor(this);
	}

	@Override
	public Optional<Duration> remaining() {
		return root().remainingTime();
	}

	@Override
	public boolean isCancelled() {
		return root().cancelled();
	}

	@Override
	public void throwIfCancelled() {
		if (isCancelled()) {
			throw new ProgressCancelledException("Operation was cancelled.");
		}
	}

	@Override
	public void cancel(final String message) {
		root().cancelFrom(this, Objects.requireNonNull(message, "message"));
	}

	@Override
	public void complete(final String message) {
		Objects.requireNonNull(message, "message");
		if (isCancelled()) {
			return;
		}
		currentMessage = message;
		if (currentAccuracy != ProgressAccuracy.INDETERMINATE) {
			currentCompleted = currentTotal;
		}
		if (this == root()) {
			root().completeFrom(this);
		} else {
			root().advanceFrom(this, rootOffset, rootWindow, () -> 1);
		}
	}

	@Override
	public void fail(final String message) {
		if (!isCancelled()) {
			root().failFrom(this, Objects.requireNonNull(message, "message"));
		}
	}

	protected final Progressor advanceToNormalized(final DoubleSupplier targetSupplier) {
		throwIfCancelled();
		root().advanceFrom(this, rootOffset, rootWindow, targetSupplier);
		return this;
	}

	protected final double respectBounds(final double position) {
		if (Double.isNaN(position) || position < 0) {
			return 0;
		}
		if (position > 1) {
			return 1;
		}
		return position;
	}

	private ProgressorImpl root() {
		return root == null ? (ProgressorImpl) this : root;
	}

	private static final class SubProgressor extends AbstractProgressor {

		private final String id;

		private SubProgressor(final ProgressorImpl root, final String parentId, final int index, final double offset,
				final double window) {
			super(root, offset, window);
			id = parentId + "_sub" + index;
		}

		@Override
		public String id() {
			return id;
		}
	}
}
