package com.retrocrawler.model.locale;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * An ISO 3166-1 alpha-2 territory code, plus stable industry release-market
 * codes such as Nintendo's {@code EUR} for Europe.
 */
public record RegionCode(String code) {

	private static final Set<String> SUPPORTED_CODES = supportedCodes();

	public RegionCode {
		code = Objects.requireNonNull(code, "code").trim().toUpperCase(Locale.ROOT);
		if (!SUPPORTED_CODES.contains(code)) {
			throw new IllegalArgumentException("Expected a supported region code: " + code);
		}
	}

	static boolean isSupported(final String code) {
		return SUPPORTED_CODES.contains(code);
	}

	private static Set<String> supportedCodes() {
		final Set<String> codes = new HashSet<>(Arrays.asList(Locale.getISOCountries()));
		codes.add("EUR");
		return Set.copyOf(codes);
	}

	@Override
	public String toString() {
		return code;
	}
}
