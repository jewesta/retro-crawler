package com.retrocrawler.core.gear.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.Configuration;
import com.retrocrawler.core.archive.Node;
import com.retrocrawler.core.gear.Confidence;

class LocalDateParserTest {

	private static final Path ARCHIVE_ROOT = Path.of("/archive");
	private static final LocalDate EXPECTED = LocalDate.of(2024, 1, 30);

	private final LocalDateParser parser = new LocalDateParser();

	@Test
	void alwaysParsesIsoCalendarDates() {
		assertEquals(EXPECTED, parser.parse("2024-01-30", context(Locale.US)).value().orElseThrow());
		assertEquals(EXPECTED, parser.parse(" 2024-01-30 ", context(Locale.GERMANY)).value().orElseThrow());
	}

	@Test
	void parsesGermanNumericOrderWithCommonSeparatorsAndText() {
		final ParseContext context = context(Locale.GERMANY);

		assertEquals(EXPECTED, parser.parse("30.01.2024", context).value().orElseThrow());
		assertEquals(EXPECTED, parser.parse("30-1-2024", context).value().orElseThrow());
		assertEquals(EXPECTED, parser.parse("30/01/2024", context).value().orElseThrow());
		assertEquals(EXPECTED, parser.parse("30. januar 2024", context).value().orElseThrow());
	}

	@Test
	void parsesAmericanNumericOrderAndText() {
		final ParseContext context = context(Locale.US);

		assertEquals(EXPECTED, parser.parse("1/30/2024", context).value().orElseThrow());
		assertEquals(EXPECTED, parser.parse("01-30-2024", context).value().orElseThrow());
		assertEquals(EXPECTED, parser.parse("January 30, 2024", context).value().orElseThrow());
	}

	@Test
	void parsesBritishLongText() {
		assertEquals(EXPECTED, parser.parse("30 January 2024", context(Locale.UK)).value().orElseThrow());
	}

	@Test
	void configuredLocaleDeterminesTheOrderOfAmbiguousNumericInput() {
		assertEquals(LocalDate.of(2024, 2, 1),
				parser.parse("01/02/2024", context(Locale.GERMANY)).value().orElseThrow());
		assertEquals(LocalDate.of(2024, 1, 2), parser.parse("01/02/2024", context(Locale.US)).value().orElseThrow());
	}

	@Test
	void rejectsImpossibleIncompleteAndTwoDigitYearValues() {
		final ParseContext context = context(Locale.GERMANY);

		assertEquals(Confidence.NONE, parser.parse("31.02.2024", context).confidence());
		assertEquals(Confidence.NONE, parser.parse("30.01.24", context).confidence());
		assertEquals(Confidence.NONE, parser.parse("2024-01", context).confidence());
		assertEquals(Confidence.NONE, parser.parse("not a date", context).confidence());
		assertTrue(parser.parse(null, context).value().isEmpty());
	}

	private static ParseContext context(final Locale locale) {
		return new ParseContext(Configuration.builder().locale(locale).build(),
				new Node(ARCHIVE_ROOT, ARCHIVE_ROOT.resolve("gear")));
	}
}
