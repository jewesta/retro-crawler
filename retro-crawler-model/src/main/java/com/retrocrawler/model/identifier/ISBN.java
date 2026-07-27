package com.retrocrawler.model.identifier;

import java.util.Locale;
import java.util.Objects;

/**
 * An ISBN-10 or ISBN-13, stored without presentation separators.
 */
public record ISBN(String value) {

	public ISBN {
		value = normalize(value);
		if (!isValidIsbn10(value) && !isValidIsbn13(value)) {
			throw new IllegalArgumentException("Invalid ISBN: " + value);
		}
	}

	public boolean isIsbn10() {
		return value.length() == 10;
	}

	public boolean isIsbn13() {
		return value.length() == 13;
	}

	@Override
	public String toString() {
		return value;
	}

	private static String normalize(final String rawValue) {
		return Objects.requireNonNull(rawValue, "value")
				.replaceAll("[\\s-]", "")
				.toUpperCase(Locale.ROOT);
	}

	private static boolean isValidIsbn10(final String value) {
		if (!value.matches("\\d{9}[\\dX]")) {
			return false;
		}

		int checksum = 0;
		for (int index = 0; index < 10; index++) {
			final int digit = index == 9 && value.charAt(index) == 'X'
					? 10
					: Character.digit(value.charAt(index), 10);
			checksum += digit * (10 - index);
		}
		return checksum % 11 == 0;
	}

	private static boolean isValidIsbn13(final String value) {
		if (!value.matches("(?:978|979)\\d{10}")) {
			return false;
		}

		int checksum = 0;
		for (int index = 0; index < 12; index++) {
			final int digit = Character.digit(value.charAt(index), 10);
			checksum += digit * (index % 2 == 0 ? 1 : 3);
		}
		final int checkDigit = (10 - checksum % 10) % 10;
		return checkDigit == Character.digit(value.charAt(12), 10);
	}
}
