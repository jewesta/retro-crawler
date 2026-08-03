package com.retrocrawler.model.temporal;

import java.time.LocalDate;
import java.time.Year;
import java.time.temporal.ChronoField;
import java.time.temporal.IsoFields;
import java.util.Locale;

/** A week in the ISO week-based calendar. */
public record YearWeek(int weekBasedYear, int week) {

	public YearWeek {
		Year.of(weekBasedYear);
		final int maximumWeek = LocalDate.of(weekBasedYear, 12, 28).get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
		if (week < 1 || week > maximumWeek) {
			throw new IllegalArgumentException(
					"ISO week must be between 1 and " + maximumWeek + " for " + weekBasedYear + ": " + week);
		}
	}

	public static YearWeek from(final LocalDate date) {
		return new YearWeek(date.get(IsoFields.WEEK_BASED_YEAR), date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
	}

	public LocalDate firstDay() {
		return LocalDate.of(weekBasedYear, 1, 4).with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, week)
				.with(ChronoField.DAY_OF_WEEK, 1);
	}

	@Override
	public String toString() {
		return String.format(Locale.ROOT, "%04d-W%02d", weekBasedYear, week);
	}
}
