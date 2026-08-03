package com.retrocrawler.model.storage;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;

public final class HardDiskDriveFormFactorParser implements FactParser {

	private static final Pattern FORM_FACTOR = Pattern.compile("^(\\d+(?:[,.]\\d+)?)\\s*(?:\"|″|in(?:ch(?:es)?)?)$",
			Pattern.CASE_INSENSITIVE);

	@Override
	public RatedFact parse(final String rawValue) {
		return parseValue(rawValue).<RatedFact> map(RatedFact::exact).orElseGet(
				() -> RatedFact.none("Expected a recognized hard-disk-drive form factor with an inch unit."));
	}

	public static Optional<HardDiskDriveFormFactor> parseValue(final String rawValue) {
		if (rawValue == null) {
			return Optional.empty();
		}
		final Matcher matcher = FORM_FACTOR.matcher(rawValue.trim());
		if (!matcher.matches()) {
			return Optional.empty();
		}
		final BigDecimal inches = new BigDecimal(matcher.group(1).replace(',', '.')).stripTrailingZeros();
		for (final HardDiskDriveFormFactor formFactor : HardDiskDriveFormFactor.values()) {
			if (formFactor.nominalInches().compareTo(inches) == 0) {
				return Optional.of(formFactor);
			}
		}
		return Optional.empty();
	}
}
