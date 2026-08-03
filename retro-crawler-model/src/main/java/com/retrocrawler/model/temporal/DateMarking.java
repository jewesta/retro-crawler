package com.retrocrawler.model.temporal;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.Objects;

/**
 * A role-neutral calendar marking which retains the precision stated by its
 * source.
 * <p>
 * A marking does not by itself claim to be a manufacture, release, publication,
 * acquisition, or lifecycle-event date.
 */
public sealed interface DateMarking {

	enum Precision {
		YEAR,
		MONTH,
		DAY,
		WEEK
	}

	Precision precision();

	Year year();

	static DateMarking of(final Year value) {
		return new YearOnly(value);
	}

	static DateMarking of(final YearMonth value) {
		return new YearAndMonth(value);
	}

	static DateMarking of(final LocalDate value) {
		return new CalendarDate(value);
	}

	static DateMarking of(final YearWeek value) {
		return new Week(value);
	}

	record YearOnly(Year value) implements DateMarking {

		public YearOnly {
			Objects.requireNonNull(value, "value");
		}

		@Override
		public Precision precision() {
			return Precision.YEAR;
		}

		@Override
		public Year year() {
			return value;
		}

		@Override
		public String toString() {
			return value.toString();
		}
	}

	record YearAndMonth(YearMonth value) implements DateMarking {

		public YearAndMonth {
			Objects.requireNonNull(value, "value");
		}

		@Override
		public Precision precision() {
			return Precision.MONTH;
		}

		@Override
		public Year year() {
			return Year.of(value.getYear());
		}

		@Override
		public String toString() {
			return value.toString();
		}
	}

	record CalendarDate(LocalDate value) implements DateMarking {

		public CalendarDate {
			Objects.requireNonNull(value, "value");
		}

		@Override
		public Precision precision() {
			return Precision.DAY;
		}

		@Override
		public Year year() {
			return Year.of(value.getYear());
		}

		@Override
		public String toString() {
			return value.toString();
		}
	}

	record Week(YearWeek value) implements DateMarking {

		public Week {
			Objects.requireNonNull(value, "value");
		}

		@Override
		public Precision precision() {
			return Precision.WEEK;
		}

		@Override
		public Year year() {
			return Year.of(value.weekBasedYear());
		}

		@Override
		public String toString() {
			return value.toString();
		}
	}
}
