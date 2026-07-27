package com.retrocrawler.model.identifier;

import java.util.Locale;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * A 48-bit MAC address in canonical colon-separated notation.
 */
public record MacAddress(String value) {

	public MacAddress {
		final String compact = Objects.requireNonNull(value, "value")
				.trim()
				.replace(":", "")
				.replace("-", "")
				.replace(".", "");
		if (!compact.matches("(?i)[0-9a-f]{12}")) {
			throw new IllegalArgumentException("Expected a 48-bit MAC address: " + value);
		}

		final String upper = compact.toUpperCase(Locale.ROOT);
		value = IntStream.range(0, 6)
				.mapToObj(index -> upper.substring(index * 2, index * 2 + 2))
				.collect(java.util.stream.Collectors.joining(":"));
	}

	public String toCompactString() {
		return value.replace(":", "");
	}

	@Override
	public String toString() {
		return value;
	}
}
