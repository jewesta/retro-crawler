package com.retrocrawler.model.temporal;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;

/** Parses canonical ISO week values such as {@code 1999-W18}. */
public final class YearWeekParser implements FactParser {

	private static final Pattern ISO_WEEK = Pattern.compile("(\\d{4})-W(\\d{2})");

	private final Clock clock;

	public YearWeekParser() {
		this(Clock.systemDefaultZone());
	}

	public YearWeekParser(final Clock clock) {
		this.clock = Objects.requireNonNull(clock, "clock");
	}

	@Override
	public RatedFact parse(final String rawValue) {
		if (rawValue == null) {
			return noMatch();
		}
		final Matcher matcher = ISO_WEEK.matcher(rawValue.trim());
		if (!matcher.matches()) {
			return noMatch();
		}

		final YearWeek value;
		try {
			value = new YearWeek(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
		} catch (final IllegalArgumentException e) {
			return noMatch();
		}

		final YearWeek current = YearWeek.from(LocalDate.now(clock));
		if (value.weekBasedYear() < YearParser.MINIMUM_YEAR.getValue()
				|| value.weekBasedYear() > current.weekBasedYear()
				|| value.weekBasedYear() == current.weekBasedYear() && value.week() > current.week()) {
			return noMatch();
		}
		return RatedFact.exact(value);
	}

	private static RatedFact noMatch() {
		return RatedFact.none("Expected an ISO week from 1950 through the current week.");
	}
}
