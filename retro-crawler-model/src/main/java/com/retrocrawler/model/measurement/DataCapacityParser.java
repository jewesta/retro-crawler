package com.retrocrawler.model.measurement;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;

public final class DataCapacityParser implements FactParser {

	private static final Pattern CAPACITY = Pattern.compile("^(\\d+(?:[,.]\\d+)?)\\s*(KB|MB|GB|TB)$",
			Pattern.CASE_INSENSITIVE);

	@Override
	public RatedFact parse(final String rawValue) {
		return parseValue(rawValue).<RatedFact> map(RatedFact::exact)
				.orElseGet(() -> RatedFact.none("Expected a positive data capacity with a KB, MB, GB, or TB unit."));
	}

	public static Optional<DataCapacity> parseValue(final String rawValue) {
		if (rawValue == null) {
			return Optional.empty();
		}

		final Matcher matcher = CAPACITY.matcher(rawValue.trim());
		if (!matcher.matches()) {
			return Optional.empty();
		}

		try {
			final BigDecimal amount = new BigDecimal(matcher.group(1).replace(',', '.'));
			final DataCapacity.Unit unit = DataCapacity.Unit.valueOf(matcher.group(2).toUpperCase(Locale.ROOT));
			return Optional.of(new DataCapacity(amount, unit));
		} catch (final IllegalArgumentException e) {
			return Optional.empty();
		}
	}
}
