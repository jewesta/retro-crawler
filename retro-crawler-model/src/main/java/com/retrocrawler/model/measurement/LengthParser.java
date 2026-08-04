package com.retrocrawler.model.measurement;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.core.gear.parser.ParseContext;
import com.retrocrawler.model.measurement.Length.Unit;

/** Parses metric and imperial scalar lengths without assigning them a role. */
public final class LengthParser implements FactParser<Length> {

	private static final Pattern LENGTH = Pattern.compile("^(\\d+(?:[,.]\\d+)?)\\s*(mm|cm|m|\"|″|in(?:ch(?:es)?)?)$",
			Pattern.CASE_INSENSITIVE);

	@Override
	public RatedFact<Length> parse(final String rawValue, final ParseContext context) {
		return parseValue(rawValue).map(RatedFact::exact)
				.orElseGet(() -> RatedFact.none("Expected a positive metric or imperial length with a unit."));
	}

	public static Optional<Length> parseValue(final String rawValue) {
		if (rawValue == null) {
			return Optional.empty();
		}
		final Matcher matcher = LENGTH.matcher(rawValue.trim());
		if (!matcher.matches()) {
			return Optional.empty();
		}

		try {
			final BigDecimal amount = new BigDecimal(matcher.group(1).replace(',', '.'));
			final Unit unit = unit(matcher.group(2));
			return Optional.of(new Length(amount, unit));
		} catch (final IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	private static Unit unit(final String rawUnit) {
		return switch (rawUnit.toLowerCase(Locale.ROOT)) {
		case "mm" -> Unit.MILLIMETER;
		case "cm" -> Unit.CENTIMETER;
		case "m" -> Unit.METER;
		case "\"", "″", "in", "inch", "inches" -> Unit.INCH;
		default -> throw new IllegalArgumentException("Unknown length unit: " + rawUnit);
		};
	}
}
