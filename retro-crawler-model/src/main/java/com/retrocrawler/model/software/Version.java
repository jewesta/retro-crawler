package com.retrocrawler.model.software;

import java.util.Objects;

/** A display version beginning with {@code v} and a decimal digit. */
public record Version(String value) {

	public Version {
		Objects.requireNonNull(value, "value");
		value = value.trim();
		if (value.startsWith("V")) {
			value = 'v' + value.substring(1);
		}
		if (!value.matches("v\\d.*")) {
			throw new IllegalArgumentException("Version must start with v followed by a digit: " + value);
		}
	}

	@Override
	public String toString() {
		return value;
	}
}
