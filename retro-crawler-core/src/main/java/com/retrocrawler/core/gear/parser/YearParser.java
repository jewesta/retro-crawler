package com.retrocrawler.core.gear.parser;

import java.time.Year;
import java.time.format.DateTimeParseException;

import com.retrocrawler.core.gear.RatedFact;

public class YearParser implements FactParser {

	public static final Year MIN = Year.of(1950);
	public static final Year MAX = Year.of(2050);

	@Override
	public RatedFact parse(final String rawValue) {
		if (rawValue.length() != 4) {
			return RatedFact.none("Expected 4 digits but was " + rawValue.length() + ".");
		}
		final Year year;
		try {
			year = Year.parse(rawValue);
		} catch (final DateTimeParseException e) {
			// Malformed
			return RatedFact.none("Value " + rawValue + " is not a valid year.");
		}
		if (year.isBefore(MIN) || year.isAfter(MAX)) {
			return RatedFact.weak(year);
		}
		return RatedFact.strong(year);
	}

}
