package com.retrocrawler.model.measurement;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.core.gear.parser.ParseContext;

public final class PowerParser implements FactParser<Power> {

	private static final Pattern POWER = Pattern.compile("([0-9]+(?:[.,][0-9]+)?)\\s*W");

	@Override
	public RatedFact<Power> parse(final String rawValue, final ParseContext context) {
		if (rawValue == null) {
			return RatedFact.none("Expected a positive electrical power in watts.");
		}

		final Matcher matcher = POWER.matcher(rawValue.trim().toUpperCase(Locale.ROOT));
		if (!matcher.matches()) {
			return RatedFact.none("Expected a positive electrical power in watts.");
		}

		try {
			return RatedFact.exact(new Power(new BigDecimal(matcher.group(1).replace(',', '.'))));
		} catch (final IllegalArgumentException e) {
			return RatedFact.none("Expected a positive electrical power in watts.");
		}
	}
}
