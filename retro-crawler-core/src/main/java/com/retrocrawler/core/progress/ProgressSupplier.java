package com.retrocrawler.core.progress;

import java.time.Duration;
import java.util.Optional;

/** Read-only progress information, suitable for monitors and snapshots. */
public interface ProgressSupplier {

	String id();

	/** Current progress on this supplier's own scale, between zero and one. */
	double progress();

	/** Current progress in the root progressor, between zero and one. */
	double overallFraction();

	ProgressStage stage();

	String message();

	long completed();

	long total();

	ProgressAccuracy accuracy();

	ProgressState state();

	Duration elapsed();

	Optional<Duration> remaining();

	default Optional<String> stepLabel() {
		return message().isBlank() ? Optional.empty() : Optional.of(message());
	}

	default long millisElapsed() {
		return elapsed().toMillis();
	}

	default long millisRemaining() {
		return remaining().map(Duration::toMillis).orElse(Long.MAX_VALUE);
	}

	default String progressMessage() {
		return progressMessage(false);
	}

	default String progressMessage(final boolean showTimeRemaining) {
		return new ProgressMessageGenerator(this).message(showTimeRemaining);
	}

	default boolean hasMadeProgress() {
		return progress() != 0;
	}

	default boolean hasFinished() {
		return state() != ProgressState.RUNNING;
	}

	/** Freezes the current values for safe use outside the progressor. */
	default ProgressSnapshot snapshot() {
		return new ProgressSnapshot(id(), stage(), message(), completed(), total(), accuracy(), state(), progress(),
				overallFraction(), elapsed(), remaining());
	}
}
