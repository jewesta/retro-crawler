package com.retrocrawler.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.retrocrawler.core.progress.ProgressAccuracy;
import com.retrocrawler.core.progress.ProgressCancelledException;
import com.retrocrawler.core.progress.ProgressStage;
import com.retrocrawler.core.progress.ProgressState;
import com.retrocrawler.core.progress.ProgressSupplier;
import com.retrocrawler.core.progress.Progressor;

/**
 * Accompanies an operation, tracking its progress and recording recoverable
 * failures.
 * <p>
 * A journal fails early by default. In {@link FailureMode#FAIL_LATE} it retains
 * every exception handed to {@link #record(Exception)} and lets the operation
 * continue wherever it can safely do so.
 * <p>
 * A journal belongs to one operation; its progress and failure history are not
 * reset for reuse.
 */
public final class Journal {

	private final Progressor progressor;

	private final FailureMode failureMode;

	private final List<Exception> failures = new ArrayList<>();

	public Journal() {
		this(Progressor.create(), FailureMode.FAIL_EARLY);
	}

	public Journal(final FailureMode failureMode) {
		this(Progressor.create(), failureMode);
	}

	public Journal(final Progressor progressor) {
		this(progressor, FailureMode.FAIL_EARLY);
	}

	public Journal(final Progressor progressor, final FailureMode failureMode) {
		this.progressor = Objects.requireNonNull(progressor, "progressor");
		this.failureMode = Objects.requireNonNull(failureMode, "failureMode");
	}

	/** Read-only access to the progress of this operation. */
	public ProgressSupplier progress() {
		return progressor;
	}

	/** Announces a determinate operation stage. */
	public Journal begin(final ProgressStage stage, final String message, final long total,
			final ProgressAccuracy accuracy) {
		requireRunning();
		progressor.begin(stage, message, total, accuracy);
		return this;
	}

	/** Announces an indeterminate operation stage. */
	public Journal indeterminate(final ProgressStage stage, final String message) {
		requireRunning();
		progressor.indeterminate(stage, message);
		return this;
	}

	/** Advances the current stage by one work unit. */
	public Journal advance(final String message) {
		requireRunning();
		progressor.advance(message);
		return this;
	}

	/** Advances the current stage to the given work-unit position. */
	public Journal advanceTo(final double position, final String message) {
		requireRunning();
		progressor.advanceTo(position, message);
		return this;
	}

	public boolean isCancelled() {
		return progressor.isCancelled();
	}

	public void throwIfCancelled() {
		progressor.throwIfCancelled();
	}

	/** Requests cancellation if the operation is still running. */
	public void cancel(final String message) {
		Objects.requireNonNull(message, "message");
		if (progressor.state() == ProgressState.RUNNING) {
			progressor.cancel(message);
		}
	}

	/**
	 * Tracks one complete operation and owns its terminal progress transition.
	 * A successful operation completes the progress; an exception fails it
	 * before the same exception is propagated. Cancellation remains a distinct
	 * terminal state.
	 */
	public <R, E extends Exception> R track(final String operationName, final Operation<R, E> operation) throws E {
		final String name = Objects.requireNonNull(operationName, "operationName").trim();
		if (name.isEmpty()) {
			throw new IllegalArgumentException("Operation name must not be empty.");
		}
		Objects.requireNonNull(operation, "operation");
		requireRunning();
		try {
			final R result = operation.perform();
			progressor.complete(name + " complete.");
			return result;
		} catch (final ProgressCancelledException cancellation) {
			throw cancellation;
		} catch (final Exception failure) {
			reportFailure(name, failure);
			throw failure;
		}
	}

	public FailureMode failureMode() {
		return failureMode;
	}

	/**
	 * Records an exception. A fail-early journal rethrows the same instance
	 * after recording it; a fail-late journal returns normally.
	 */
	public <E extends Exception> void record(final E failure) throws E {
		Objects.requireNonNull(failure, "failure");
		synchronized (failures) {
			failures.add(failure);
		}
		if (failureMode == FailureMode.FAIL_EARLY) {
			throw failure;
		}
	}

	/** Every exception recorded so far, in encounter order. */
	public List<Exception> failures() {
		synchronized (failures) {
			return List.copyOf(failures);
		}
	}

	public int failureCount() {
		synchronized (failures) {
			return failures.size();
		}
	}

	public boolean hasFailures() {
		return failureCount() > 0;
	}

	private void requireRunning() {
		progressor.throwIfCancelled();
		if (progressor.state() != ProgressState.RUNNING) {
			throw new IllegalStateException("Journal operation has already finished.");
		}
	}

	private void reportFailure(final String operationName, final Exception failure) {
		try {
			progressor.fail(operationName + " failed: " + failureDescription(failure));
		} catch (final RuntimeException reportingFailure) {
			if (reportingFailure != failure) {
				failure.addSuppressed(reportingFailure);
			}
		}
	}

	private static String failureDescription(final Exception failure) {
		final String message = failure.getMessage();
		return message == null || message.isBlank() ? failure.getClass().getSimpleName() : message;
	}

	@FunctionalInterface
	public interface Operation<R, E extends Exception> {

		R perform() throws E;
	}

}
