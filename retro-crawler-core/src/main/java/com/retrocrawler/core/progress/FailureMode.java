package com.retrocrawler.core.progress;

/**
 * Decides whether an exception deliberately handed to a {@link Progressor}
 * aborts an operation immediately.
 */
public enum FailureMode {

	/** Record the exception, then rethrow it unchanged. */
	FAIL_EARLY,

	/**
	 * Record the exception and let the operation continue to its reporting
	 * boundary.
	 */
	FAIL_LATE
}
