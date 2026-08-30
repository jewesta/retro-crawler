package com.retrocrawler.core.crawl;

import java.util.Objects;

/** Stable failure description retained after a crawl operation finishes. */
public record CrawlOperationFailure(String type, String message) {

	public CrawlOperationFailure {
		type = requireText(type, "type");
		message = requireText(message, "message");
	}

	static CrawlOperationFailure from(final Throwable failure) {
		Objects.requireNonNull(failure, "failure");
		final String description = failure.getMessage();
		return new CrawlOperationFailure(failure.getClass().getName(),
				description == null || description.isBlank() ? failure.getClass().getSimpleName() : description);
	}

	private static String requireText(final String value, final String name) {
		final String text = Objects.requireNonNull(value, name).trim();
		if (text.isEmpty()) {
			throw new IllegalArgumentException("Crawl operation failure " + name + " must not be empty.");
		}
		return text;
	}
}
