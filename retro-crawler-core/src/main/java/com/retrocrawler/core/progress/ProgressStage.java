package com.retrocrawler.core.progress;

import java.util.Locale;
import java.util.Objects;

/**
 * Extensible identifier for the current stage of an operation.
 */
public record ProgressStage(String value) {

	public static final ProgressStage IDLE = new ProgressStage("IDLE");

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
