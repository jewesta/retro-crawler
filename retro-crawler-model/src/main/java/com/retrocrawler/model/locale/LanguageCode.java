package com.retrocrawler.model.locale;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A two-letter ISO 639-1 language code.
 */
public record LanguageCode(String code) {

	private static final Set<String> ISO_639_1 = Arrays.stream(Locale.getISOLanguages())
			.map(value -> value.toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());

	public LanguageCode {
		code = Objects.requireNonNull(code, "code").trim().toLowerCase(Locale.ROOT);
		if (!ISO_639_1.contains(code)) {
			throw new IllegalArgumentException("Expected an ISO 639-1 language code: " + code);
		}
	}

	static boolean isSupported(final String code) {
		return ISO_639_1.contains(code);
	}

	@Override
	public String toString() {
		return code;
	}
}
