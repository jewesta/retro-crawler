package com.retrocrawler.model.temporal;

import java.time.Clock;
import java.time.Year;
import java.time.format.DateTimeParseException;
import java.util.Objects;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParseContext;
import com.retrocrawler.core.gear.parser.FactParser;

/**
 * Parses plausible four-digit years without treating future numbers as dates.
 */
public final class YearParser implements FactParser<Year> {

	public static final Year MINIMUM_YEAR = Year.of(1950);

	private final Clock clock;

	public YearParser() {
		this(Clock.systemDefaultZone());
	}

	public YearParser(final Clock clock) {
		this.clock = Objects.requireNonNull(clock, "clock");
	}

	@Override
	public RatedFact<Year> parse(final String rawValue, final FactParseContext context) {
		if (rawValue == null || !rawValue.trim().matches("\\d{4}")) {
			return noMatch();
		}

		final Year year;
		try {
			year = Year.parse(rawValue.trim());
		} catch (final DateTimeParseException e) {
			return noMatch();
		}

		if (year.isBefore(MINIMUM_YEAR) || year.isAfter(Year.now(clock))) {
			return RatedFact.none("Expected a year from 1950 through the current year.");
		}
		return RatedFact.strong(year);
	}

	private static RatedFact<Year> noMatch() {
		return RatedFact.none("Expected a four-digit year from 1950 through the current year.");
	}
}
