package com.retrocrawler.core;

import java.util.List;
import java.util.Objects;

import com.retrocrawler.core.util.RetroCrawlerException;

/** Reports every exception recorded during a fail-late crawl. */
@SuppressWarnings("serial")
public final class CrawlException extends RetroCrawlerException {

	private static final int DISPLAY_LIMIT = 50;

	private final List<Exception> failures;

	public CrawlException(final List<? extends Exception> failures) {
		super(message(failures));
		this.failures = List.copyOf(failures);
		if (this.failures.isEmpty()) {
			throw new IllegalArgumentException("Require at least one recorded exception.");
		}
	}

	/**
	 * Every recorded exception, including those omitted from
	 * {@link #getMessage()}.
	 */
	public List<Exception> failures() {
		return failures;
	}

	private static String message(final List<? extends Exception> failures) {
		Objects.requireNonNull(failures, "failures");
		if (failures.isEmpty()) {
			return "Crawl failed without a recorded exception.";
		}

		final int displayed = Math.min(failures.size(), DISPLAY_LIMIT);
		final StringBuilder message = new StringBuilder("Crawl failed with ").append(failures.size())
				.append(failures.size() == 1 ? " recorded exception" : " recorded exceptions").append("; showing ")
				.append(displayed).append(':');
		for (int index = 0; index < displayed; index++) {
			message.append('\n').append(index + 1).append(". ").append(describe(failures.get(index)));
		}
		final int omitted = failures.size() - displayed;
		if (omitted > 0) {
			message.append('\n').append(omitted)
					.append(omitted == 1 ? " further exception is not shown." : " further exceptions are not shown.");
		}
		return message.toString();
	}

	private static String describe(final Exception failure) {
		Objects.requireNonNull(failure, "failure");
		final String description = failure.getMessage();
		return failure.getClass().getSimpleName()
				+ (description == null || description.isBlank() ? "" : ": " + description);
	}
}
