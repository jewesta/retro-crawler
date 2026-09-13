package com.retrocrawler.model.hardware;

import java.util.Objects;

/**
 * An AMD production marking consisting of a generation-dependent prefix, a
 * three- or four-digit date code, and an opaque production suffix.
 * <p>
 * The prefix and suffix are deliberately not interpreted because their meaning
 * varies between processor generations and is not completely public.
 */
public record AmdProcessorMarking(String prefix, String dateCode, String suffix) implements ProcessorMarking {

	public AmdProcessorMarking {
		prefix = requireMatch(prefix, "[A-Z][A-Z0-9]?", "AMD production-marking prefix");
		dateCode = requireMatch(dateCode, "\\d{3,4}", "AMD production date code");
		suffix = requireMatch(suffix, "[A-Z0-9]{3,4}(?:-[A-Z])?", "AMD production-marking suffix");
	}

	private static String requireMatch(final String value, final String pattern, final String description) {
		final String required = Objects.requireNonNull(value, description);
		if (!required.matches(pattern)) {
			throw new IllegalArgumentException("Invalid " + description + ": " + value);
		}
		return required;
	}

	@Override
	public String toString() {
		return prefix + "-" + dateCode + suffix;
	}
}
