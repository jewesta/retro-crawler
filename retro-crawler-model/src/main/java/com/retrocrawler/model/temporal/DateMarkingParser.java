package com.retrocrawler.model.temporal;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Objects;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParseContext;
import com.retrocrawler.core.gear.parser.FactParser;

/** Parses role-neutral date markings in canonical ISO forms. */
public final class DateMarkingParser implements FactParser<DateMarking> {

	private final Clock clock;
	private final YearWeekParser yearWeekParser;

	public DateMarkingParser() {
		this(Clock.systemDefaultZone());
	}

	public DateMarkingParser(final Clock clock) {
		this.clock = Objects.requireNonNull(clock, "clock");
		yearWeekParser = new YearWeekParser(clock);
	}

	@Override
	public RatedFact<DateMarking> parse(final String rawValue, final FactParseContext context) {
		if (rawValue == null) {
			return noMatch();
		}
		final String value = rawValue.trim();

		try {
			if (value.matches("\\d{4}")) {
				final Year year = Year.parse(value);
				if (year.isBefore(YearParser.MINIMUM_YEAR) || year.isAfter(Year.now(clock))) {
					return noMatch();
				}
				return RatedFact.strong(DateMarking.of(year));
			}
			if (value.matches("\\d{4}-\\d{2}")) {
				final YearMonth month = YearMonth.parse(value);
				if (month.isBefore(YearMonth.from(YearParser.MINIMUM_YEAR.atDay(1)))
						|| month.isAfter(YearMonth.now(clock))) {
					return noMatch();
				}
				return RatedFact.exact(DateMarking.of(month));
			}
			if (value.matches("\\d{4}-\\d{2}-\\d{2}")) {
				final LocalDate date = LocalDate.parse(value);
				if (date.isBefore(YearParser.MINIMUM_YEAR.atDay(1)) || date.isAfter(LocalDate.now(clock))) {
					return noMatch();
				}
				return RatedFact.exact(DateMarking.of(date));
			}
		} catch (final DateTimeParseException e) {
			return noMatch();
		}

		final RatedFact<YearWeek> week = yearWeekParser.parse(value, context);
		return week.value().map(parsed -> RatedFact.exact(DateMarking.of(parsed)))
				.orElseGet(DateMarkingParser::noMatch);
	}

	private static RatedFact<DateMarking> noMatch() {
		return RatedFact.none("Expected YYYY, YYYY-MM, YYYY-MM-DD, or YYYY-Www from 1950 through today.");
	}
}
