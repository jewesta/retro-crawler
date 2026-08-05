package com.retrocrawler.model.measurement;

import static com.retrocrawler.model.ParserTestContext.CONTEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.gear.Confidence;

class TrackDensityTest {

	@Test
	void parsesTracksPerInchWithoutGuessingFromBareNumbers() {
		final TrackDensityParser parser = new TrackDensityParser();

		assertEquals(new TrackDensity(48), parser.parse("48TPI", CONTEXT).value().orElseThrow());
		assertEquals(new TrackDensity(96), parser.parse("96 tpi", CONTEXT).value().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("96", CONTEXT).confidence());
	}

	@Test
	void requiresAPositiveTrackDensity() {
		assertThrows(IllegalArgumentException.class, () -> new TrackDensity(0));
		assertEquals(Confidence.NONE, new TrackDensityParser().parse("0TPI", CONTEXT).confidence());
	}
}
