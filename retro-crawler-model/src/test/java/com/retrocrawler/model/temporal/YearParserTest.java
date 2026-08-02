package com.retrocrawler.model.temporal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.Year;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Confidence;

class YearParserTest {

	private static final Clock DURING_2026 = Clock.fixed(Instant.parse("2026-08-02T12:00:00Z"), ZoneOffset.UTC);

	@Test
	void acceptsOnlyFourDigitYearsFrom1950ThroughTheCurrentYear() {
		final YearParser parser = new YearParser(DURING_2026);

		assertEquals(Year.of(1950), parser.parse("1950").getValue().orElseThrow());
		assertEquals(Year.of(2026), parser.parse("2026").getValue().orElseThrow());
		assertEquals(Confidence.STRONG, parser.parse("1989").getConfidence());
		assertEquals(Confidence.NONE, parser.parse("1949").getConfidence());
		assertEquals(Confidence.NONE, parser.parse("2027").getConfidence());
		assertEquals(Confidence.NONE, parser.parse("89").getConfidence());
	}
}
