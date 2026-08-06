package com.retrocrawler.core.progress;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Immutable view of an operation's current progress.
 *
 * <p>
 * Adapted from progressor code developed by Relimit GmbH. Used in RetroCrawler
 * with permission.
 *
 * @param id
 *            stable ID of the root progressor
 * @param stage
 *            current operation stage
 * @param message
 *            human-readable status
 * @param completed
 *            completed stage work units, or {@code -1}
 * @param total
 *            total stage work units, or {@code -1}
 * @param accuracy
 *            accuracy of the stage work units
 * @param state
 *            operation state
 * @param progress
 *            position in the observed progressor, from zero to one
 * @param overallFraction
 *            position in the root progressor, from zero to one
 * @param elapsed
 *            elapsed time in the current stage
 * @param remaining
 *            estimated remaining time in the current stage
 */
public record ProgressSnapshot(String id, ProgressStage stage, String message, long completed, long total,
		ProgressAccuracy accuracy, ProgressState state, double progress, double overallFraction, Duration elapsed,
		Optional<Duration> remaining) implements ProgressSupplier {

	public ProgressSnapshot {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(stage, "stage");
		Objects.requireNonNull(message, "message");
		Objects.requireNonNull(accuracy, "accuracy");
		Objects.requireNonNull(state, "state");
		Objects.requireNonNull(elapsed, "elapsed");
		remaining = Objects.requireNonNull(remaining, "remaining");

		if (!Double.isFinite(progress) || progress < 0 || progress > 1) {
			throw new IllegalArgumentException("Progress fraction must be between zero and one.");
		}
		if (!Double.isFinite(overallFraction) || overallFraction < 0 || overallFraction > 1) {
			throw new IllegalArgumentException("Overall progress fraction must be between zero and one.");
		}

		final boolean indeterminate = completed == -1 && total == -1 && accuracy == ProgressAccuracy.INDETERMINATE;
		final boolean determinate = completed >= 0 && total >= 0 && completed <= total
				&& accuracy != ProgressAccuracy.INDETERMINATE;
		if (!indeterminate && !determinate) {
			throw new IllegalArgumentException("Progress must either be indeterminate or satisfy "
					+ "0 <= completed <= total with exact or approximate accuracy.");
		}
		if (accuracy == ProgressAccuracy.INDETERMINATE && remaining.isPresent()) {
			throw new IllegalArgumentException("Indeterminate progress cannot estimate remaining time.");
		}
	}

	public boolean isDeterminate() {
		return accuracy != ProgressAccuracy.INDETERMINATE;
	}

	@Override
	public ProgressSnapshot snapshot() {
		return this;
	}

	public OptionalDouble stageFraction() {
		if (!isDeterminate() || total == 0) {
			return OptionalDouble.empty();
		}
		return OptionalDouble.of((double) completed / total);
	}

	@Override
	public boolean hasFinished() {
		return state != ProgressState.RUNNING;
	}
}
