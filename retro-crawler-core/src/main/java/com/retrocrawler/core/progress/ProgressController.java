package com.retrocrawler.core.progress;

import java.time.Duration;
import java.util.Collection;
import java.util.function.Supplier;

/** Controls progress without exposing its read-only observation contract. */
public interface ProgressController extends ProgressSplitter {

	String id();

	ProgressController reset();

	ProgressController reset(long maximum);

	default ProgressController advance() {
		return advanceBy(1);
	}

	ProgressController advanceBy(double delta);

	ProgressController advanceBy(double delta, String message);

	ProgressController advanceTo(double position);

	ProgressController advanceTo(double position, String message);

	ProgressController advanceToBase(double position, double maximum);

	ProgressController advanceToBase1(double position);

	ProgressController advanceToBase100(double percentage);

	ProgressController advanceToEnd();

	ProgressController advanceToEnd(String message);

	ProgressController begin(ProgressStage stage, String message, long total, ProgressAccuracy accuracy);

	ProgressController indeterminate(ProgressStage stage, String message);

	@Override
	ProgressController setStepLabel(String label);

	ProgressController withMonitor(ProgressMonitor monitor);

	AutoProgressor autoProgress(long millis);

	default AutoProgressor autoProgress(final Duration duration) {
		return autoProgress(duration.toMillis());
	}

	boolean isCancelled();

	void throwIfCancelled();

	void cancel(String message);

	void complete(String message);

	void fail(String message);

	default <T> CollectionProgressor<T> follow(final Collection<T> collection) {
		return new CollectionProgressor<>(this, collection, null);
	}

	default <T> CollectionProgressor<T> follow(final Collection<T> collection, final String stepLabel) {
		return new CollectionProgressor<>(this, collection, stepLabel);
	}

	default void followAndRun(final Collection<Runnable> runnables) {
		follow(runnables).forEach(Runnable::run);
	}

	default void followAndRun(final Collection<Runnable> runnables, final String stepLabel) {
		follow(runnables, stepLabel).forEach(Runnable::run);
	}

	default <T> T untilTheEnd(final Supplier<T> supplier) {
		final T result = supplier.get();
		advanceToEnd();
		return result;
	}
}
