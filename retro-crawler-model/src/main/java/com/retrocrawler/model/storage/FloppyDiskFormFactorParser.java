package com.retrocrawler.model.storage;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;

public final class FloppyDiskFormFactorParser implements FactParser {

	private static final Pattern DECIMAL_INCHES = Pattern.compile(
			"^(\\d+(?:[,.]\\d+)?)\\s*(?:\"|″|in(?:ch(?:es)?)?)$",
			Pattern.CASE_INSENSITIVE);

	@Override
	public RatedFact parse(final String rawValue) {
		return parseValue(rawValue).<RatedFact>map(RatedFact::exact)
				.orElseGet(() -> RatedFact.none("Expected a recognized nominal floppy-disk form factor with an inch unit."));
	}

	public static Optional<FloppyDiskFormFactor> parseValue(final String rawValue) {
		if (rawValue == null) {
			return Optional.empty();
		}

		final String trimmed = rawValue.trim();
		if ("3½\"".equals(trimmed) || "3½″".equals(trimmed)) {
			return Optional.of(FloppyDiskFormFactor.INCH_3_5);
		}
		if ("5¼\"".equals(trimmed) || "5¼″".equals(trimmed)) {
			return Optional.of(FloppyDiskFormFactor.INCH_5_25);
		}

		final Matcher matcher = DECIMAL_INCHES.matcher(trimmed);
		if (!matcher.matches()) {
			return Optional.empty();
		}
		final BigDecimal inches = new BigDecimal(matcher.group(1).replace(',', '.')).stripTrailingZeros();
		for (final FloppyDiskFormFactor formFactor : FloppyDiskFormFactor.values()) {
			if (formFactor.nominalInches().compareTo(inches) == 0) {
				return Optional.of(formFactor);
			}
		}
		return Optional.empty();
	}
}
