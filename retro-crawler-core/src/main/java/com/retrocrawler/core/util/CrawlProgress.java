package com.retrocrawler.core.util;

import java.util.Objects;
import java.util.OptionalDouble;

/**
 * Structured progress reported while an archive is planned, crawled, stowed
 * away, and resolved.
 *
 * @param phase       current phase
 * @param message     human-readable status
 * @param completed   completed work units, or {@code -1} when indeterminate
 * @param total       total work units, or {@code -1} when indeterminate
 * @param approximate whether the work units are deliberately approximate
 */
public record CrawlProgress(Phase phase, String message, long completed, long total, boolean approximate) {

	public enum Phase {
		PLANNING,
		CRAWLING,
		STOWING,
		RESOLVING,
		COMPLETE,
		CANCELLED
	}

	public CrawlProgress {
		Objects.requireNonNull(phase, "phase");
		Objects.requireNonNull(message, "message");
		final boolean indeterminate = completed == -1 && total == -1;
		final boolean determinate = completed >= 0 && total >= 0 && completed <= total;
		if (!indeterminate && !determinate) {
			throw new IllegalArgumentException("Progress must either be indeterminate (-1/-1) or satisfy "
					+ "0 <= completed <= total.");
		}
		if (indeterminate && approximate) {
			throw new IllegalArgumentException("Indeterminate progress cannot be marked approximate.");
		}
	}

	public static CrawlProgress indeterminate(final Phase phase, final String message) {
		return new CrawlProgress(phase, message, -1, -1, false);
	}

	public static CrawlProgress approximate(final Phase phase, final String message, final long completed,
			final long total) {
		return new CrawlProgress(phase, message, completed, total, true);
	}

	public static CrawlProgress exact(final Phase phase, final String message, final long completed, final long total) {
		return new CrawlProgress(phase, message, completed, total, false);
	}

	public boolean isDeterminate() {
		return total >= 0;
	}

	public OptionalDouble fraction() {
		if (!isDeterminate() || total == 0) {
			return OptionalDouble.empty();
		}
		return OptionalDouble.of((double) completed / total);
	}
}
