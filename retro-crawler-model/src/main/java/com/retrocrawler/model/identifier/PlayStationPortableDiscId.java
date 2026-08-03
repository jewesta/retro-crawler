package com.retrocrawler.model.identifier;

import java.util.Objects;

/**
 * A physical PlayStation Portable software disc ID.
 */
public record PlayStationPortableDiscId(PlayStationPortableDiscPrefix prefix, String number) {

	public PlayStationPortableDiscId {
		Objects.requireNonNull(prefix, "prefix");
		number = Objects.requireNonNull(number, "number").trim();
		if (!number.matches("\\d{5}")) {
			throw new IllegalArgumentException("Expected a five-digit PSP disc number: " + number);
		}
	}

	public String toCompactString() {
		return prefix.name() + number;
	}

	@Override
	public String toString() {
		return prefix.name() + "-" + number;
	}
}
