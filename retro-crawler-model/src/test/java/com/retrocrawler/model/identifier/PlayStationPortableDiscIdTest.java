package com.retrocrawler.model.identifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Confidence;

class PlayStationPortableDiscIdTest {

	private final PlayStationPortableDiscIdParser parser = new PlayStationPortableDiscIdParser();

	@Test
	void normalizesKnownPhysicalDiscIdSpellings() {
		final PlayStationPortableDiscId european = new PlayStationPortableDiscId(PlayStationPortableDiscPrefix.ULES,
				"01234");

		assertEquals(european, parser.parse("ules-01234").value().orElseThrow());
		assertEquals(european, parser.parse("ULES 01234").value().orElseThrow());
		assertEquals(european, parser.parse("ULES01234").value().orElseThrow());
		assertEquals("ULES-01234", european.toString());
		assertEquals("ULES01234", european.toCompactString());
		assertEquals(PlayStationPortableMarket.EUROPE, european.prefix().market());
		assertEquals(PlayStationPortablePublishingClass.LICENSED, european.prefix().publishingClass());
	}

	@Test
	void coversTheKnownPhysicalPrefixMatrixAndRejectsOtherPlayStationIds() {
		assertEquals(10, PlayStationPortableDiscPrefix.values().length);
		assertEquals(PlayStationPortableMarket.JAPAN, parsed("UCJS-00001").prefix().market());
		assertEquals(PlayStationPortablePublishingClass.SONY_COMPUTER_ENTERTAINMENT,
				parsed("UCUS-00001").prefix().publishingClass());
		assertEquals(PlayStationPortableMarket.KOREA, parsed("ULKS-00001").prefix().market());
		assertEquals(PlayStationPortableMarket.ASIA, parsed("UCAS-00001").prefix().market());

		assertEquals(Confidence.NONE, parser.parse("CUSA-00001").confidence());
		assertEquals(Confidence.NONE, parser.parse("ULES-1234").confidence());
		assertEquals(Confidence.NONE, parser.parse("ULES01234DATA").confidence());
	}

	private PlayStationPortableDiscId parsed(final String rawValue) {
		return assertInstanceOf(PlayStationPortableDiscId.class, parser.parse(rawValue).value().orElseThrow());
	}
}
