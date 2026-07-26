package com.retrocrawler.mycollection.model;

import java.util.Locale;
import java.util.Objects;

public record FloppyImageId(String value) {

	public FloppyImageId {
		value = Objects.requireNonNull(value, "value").toUpperCase(Locale.ROOT);
		if (!value.matches("FD-\\d+")) {
			throw new IllegalArgumentException("Floppy image ID must use the FD-<number> namespace: " + value);
		}
	}

	@Override
	public String toString() {
		return value;
	}
}
