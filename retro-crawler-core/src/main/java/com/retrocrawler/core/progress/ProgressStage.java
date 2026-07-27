package com.retrocrawler.core.progress;

import java.util.Locale;
import java.util.Objects;

/**
 * Extensible identifier for the current stage of an operation.
 */
public record ProgressStage(String value) {

	public static final ProgressStage IDLE = new ProgressStage("IDLE");
	public static final ProgressStage PLANNING = new ProgressStage("PLANNING");
	public static final ProgressStage CRAWLING = new ProgressStage("CRAWLING");
	public static final ProgressStage STOWING = new ProgressStage("STOWING");
	public static final ProgressStage RESOLVING = new ProgressStage("RESOLVING");

	public ProgressStage {
		value = Objects.requireNonNull(value, "value").trim().toUpperCase(Locale.ROOT);
		if (value.isEmpty()) {
			throw new IllegalArgumentException("Progress stage must not be empty.");
		}
	}

	public static ProgressStage of(final String value) {
		return new ProgressStage(value);
	}

	@Override
	public String toString() {
		return value;
	}
}
