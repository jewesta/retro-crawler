package com.retrocrawler.core.progress;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Full progress contract: control, splitting and read-only observation.
 *
 * <p>
 * Adapted from Progressor in PEPPER 2. Its external text, logging, collection
 * and dependency-injection integrations deliberately remain outside the core.
 */
public interface Progressor extends ProgressController, ProgressSupplier {

	/** A thread-safe progressor which deliberately does nothing. */
	Progressor DUMMY = new DummyProgressor();

	static Progressor create() {
		return new ProgressorImpl();
	}

	static Progressor observing(final ProgressMonitor monitor) {
		return create().withMonitor(monitor);
	}

	static Progressor reportingMessages(final Consumer<String> messageConsumer) {
		Objects.requireNonNull(messageConsumer, "messageConsumer");
		return observing(progress -> messageConsumer.accept(progress.message()));
	}

	@Override
	Progressor reset();

	@Override
	Progressor reset(long maximum);

	@Override
	default Progressor advance() {
		return advanceBy(1);
	}

	@Override
	Progressor advanceBy(double delta);

	@Override
	Progressor advanceBy(double delta, String message);

	@Override
	Progressor advanceTo(double position);

	@Override
	Progressor advanceTo(double position, String message);

	@Override
	Progressor advanceToBase(double position, double maximum);

	@Override
	Progressor advanceToBase1(double position);

	@Override
	Progressor advanceToBase100(double percentage);

	@Override
	Progressor advanceToEnd();

	@Override
	Progressor advanceToEnd(String message);

	@Override
	Progressor begin(ProgressStage stage, String message, long total, ProgressAccuracy accuracy);

	@Override
	Progressor indeterminate(ProgressStage stage, String message);

	@Override
	Progressor setStepLabel(String label);

	@Override
	Progressor withMonitor(ProgressMonitor monitor);

	final class DummyProgressor extends ProgressorImpl {

		private DummyProgressor() {
			super("0000");
		}

		@Override
		public Progressor[] splitByPercentages(final BigDecimal... percentages) {
			return dummies(percentages.length);
		}

		@Override
		public Progressor[] splitByPercentages(final Collection<BigDecimal> percentages) {
			return dummies(percentages.size());
		}

		@Override
		public Progressor[] splitIntoEqualParts(final int parts) {
			return dummies(parts);
		}

		@Override
		public Progressor[] splitInRelationTo(final Collection<Double> segments) {
			return dummies(segments.size());
		}

		@Override
		public Progressor[] splitInRelationTo(final double... segments) {
			return dummies(segments.length);
		}

		@Override
		public Progressor reset(final long maximum) {
			return this;
		}

		@Override
		public Progressor begin(final ProgressStage stage, final String message, final long total,
				final ProgressAccuracy accuracy) {
			return this;
		}

		@Override
		public Progressor indeterminate(final ProgressStage stage, final String message) {
			return this;
		}

		@Override
		public Progressor advanceToBase(final double position, final double maximum) {
			return this;
		}

		@Override
		public Progressor advanceToBase1(final double position) {
			return this;
		}

		@Override
		public Progressor advanceBy(final double delta, final String message) {
			return this;
		}

		@Override
		public Progressor advanceTo(final double position, final String message) {
			return this;
		}

		@Override
		public Progressor advanceToEnd(final String message) {
			return this;
		}

		@Override
		public Progressor setStepLabel(final String label) {
			return this;
		}

		@Override
		public Progressor withMonitor(final ProgressMonitor monitor) {
			return this;
		}

		@Override
		public AutoProgressor autoProgress(final long millis) {
			return new AutoProgressor() {

				@Override
				public void run() {
					// No operation.
				}

				@Override
				public void done() {
					// No operation.
				}

				@Override
				public AutoProgressor updateInterval(final long intervalMillis) {
					return this;
				}
			};
		}

		@Override
		public void cancel(final String message) {
			// No operation.
		}

		@Override
		public void complete(final String message) {
			// No operation.
		}

		@Override
		public void fail(final String message) {
			// No operation.
		}

		private static Progressor[] dummies(final int size) {
			if (size < 0) {
				throw new IllegalArgumentException("Progress part count must not be negative.");
			}
			final Progressor[] dummies = new Progressor[size];
			Arrays.fill(dummies, DUMMY);
			return dummies;
		}
	}
}
