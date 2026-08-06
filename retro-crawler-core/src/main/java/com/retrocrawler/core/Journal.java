package com.retrocrawler.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

	public Progressor progressor() {
		return progressor;
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

}
