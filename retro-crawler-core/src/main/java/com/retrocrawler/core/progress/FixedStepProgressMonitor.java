package com.retrocrawler.core.progress;

import java.util.Objects;
import java.util.OptionalDouble;

/**
 * Reduces determinate progress observations to a fixed number of steps.
 *
 * <p>
 * Stage and state changes are always reported. Indeterminate updates are
 * reported with a step of {@code -1}; determinate updates use a step between
 * zero and the configured maximum, inclusive.
 *
 * <p>
 * Adapted from progressor code developed by Relimit GmbH. Used in RetroCrawler
 * with permission.
 */
public abstract class FixedStepProgressMonitor implements ProgressMonitor {

	private final long maximumStep;

	private ProgressStage previousStage;

	private ProgressState previousState;

	private long previousStep = Long.MIN_VALUE;

	protected FixedStepProgressMonitor(final long maximumStep) {
		if (maximumStep <= 0) {
			throw new IllegalArgumentException("Maximum progress step must be positive.");
		}
		this.maximumStep = maximumStep;
	}

	@Override
	public final synchronized void onProgress(final ProgressSnapshot progress) {
		Objects.requireNonNull(progress, "progress");
		final long currentStep = step(progress);
		final boolean stageChanged = !progress.stage().equals(previousStage);
		final boolean stateChanged = progress.state() != previousState;
		if (!stageChanged && !stateChanged && currentStep == previousStep) {
			return;
		}

		onProgressStep(maximumStep, currentStep, progress);
		previousStage = progress.stage();
		previousState = progress.state();
		previousStep = currentStep;
	}

	protected abstract void onProgressStep(long maximumStep, long currentStep, ProgressSnapshot progress);

	private long step(final ProgressSnapshot progress) {
		final OptionalDouble fraction = progress.stageFraction();
		if (fraction.isEmpty()) {
			return -1;
		}
		return Math.min(maximumStep, (long) Math.floor(fraction.getAsDouble() * maximumStep));
	}
}
