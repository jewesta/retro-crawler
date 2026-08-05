package com.retrocrawler.model.software;

import static com.retrocrawler.model.ParserTestContext.CONTEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.gear.Confidence;

class VersionTest {

	@Test
	void parsesVersionMarkersWithoutInventingMeaningForBareNumbers() {
		final VersionParser parser = new VersionParser();

		assertEquals(new Version("v5.0"), parser.parse("v5.0", CONTEXT).value().orElseThrow());
		assertEquals(new Version("v6.02 beta 1"), parser.parse("V6.02 beta 1", CONTEXT).value().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("5.0", CONTEXT).confidence());
		assertEquals(Confidence.NONE, parser.parse("version 5", CONTEXT).confidence());
		assertThrows(IllegalArgumentException.class, () -> new Version("vNext"));
	}
}
