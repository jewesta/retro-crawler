package com.retrocrawler.model.temporal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Confidence;

class DateMarkingParserTest {

	private static final Clock DURING_2026 = Clock.fixed(Instant.parse("2026-08-02T12:00:00Z"), ZoneOffset.UTC);

	@Test
	void preservesThePrecisionOfCanonicalIsoMarkings() {
		final DateMarkingParser parser = new DateMarkingParser(DURING_2026);

		assertEquals(DateMarking.of(Year.of(1994)), parser.parse("1994").value().orElseThrow());
		assertEquals(Confidence.STRONG, parser.parse("1994").confidence());
		assertEquals(DateMarking.of(YearMonth.of(1994, 5)),
				parser.parse("1994-05").value().orElseThrow());
		assertEquals(DateMarking.of(LocalDate.of(1994, 5, 12)),
				parser.parse("1994-05-12").value().orElseThrow());
		assertEquals(DateMarking.of(new YearWeek(1994, 5)),
				parser.parse("1994-W05").value().orElseThrow());
		assertEquals(DateMarking.Precision.MONTH,
				((DateMarking) parser.parse("1994-05").value().orElseThrow()).precision());
		assertEquals(DateMarking.Precision.WEEK,
				((DateMarking) parser.parse("1994-W05").value().orElseThrow()).precision());
	}

	@Test
	void distinguishesMonthsFromWeeksAndRejectsNoncanonicalOrInvalidDates() {
		final DateMarkingParser parser = new DateMarkingParser(DURING_2026);

		assertEquals("1994-05", parser.parse("1994-05").value().orElseThrow().toString());
		assertEquals("1994-W05", parser.parse("1994-W05").value().orElseThrow().toString());
		for (final String invalid : new String[] {
				"1994-KW05", "KW05 1994", "05-12-1994", "1994-13", "1994-02-29",
				"1949", "2027", "2026-09", "2026-08-03", "2026-W32" }) {
			assertEquals(Confidence.NONE, parser.parse(invalid).confidence(), invalid);
		}
	}

	@Test
	void validatesIsoWeekYearsAndExposesTheirFirstDay() {
		assertEquals(LocalDate.of(1994, 8, 1), new YearWeek(1994, 31).firstDay());
		assertEquals(new YearWeek(2020, 53), YearWeek.from(LocalDate.of(2020, 12, 31)));
		assertThrows(IllegalArgumentException.class, () -> new YearWeek(2021, 53));
	}
}
