package com.retrocrawler.model.identifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Confidence;

class IdentifierParsersTest {

	@Test
	void validatesAndNormalizesIsbn10AndIsbn13() {
		final ISBNParser parser = new ISBNParser();

		assertEquals(new ISBN("0306406152"), parser.parse("0-306-40615-2").getValue().orElseThrow());
		assertEquals(new ISBN("9780306406157"), parser.parse("978-0-306-40615-7").getValue().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("978-0-306-40615-8").getConfidence());
		assertEquals(Confidence.NONE, parser.parse("not-an-isbn").getConfidence());
	}

	@Test
	void normalizesCommonMacAddressNotations() {
		final MacAddressParser parser = new MacAddressParser();
		final MacAddress expected = new MacAddress("00:00:C0:0D:66:AB");

		assertEquals(expected, parser.parse("00-00-c0-0d-66-ab").getValue().orElseThrow());
		assertEquals(expected, parser.parse("0000.c00d.66ab").getValue().orElseThrow());
		assertEquals("0000C00D66AB", expected.toCompactString());
		assertEquals(Confidence.NONE, parser.parse("00:00:C0:0D:66").getConfidence());
	}

	@Test
	void parsesTheRetroWebIdsAsReusableExternalReferences() {
		final TheRetroWebIdParser parser = new TheRetroWebIdParser();

		assertEquals(new TheRetroWebId(10_510), parser.parse(" 10510 ").getValue().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("motherboard-10510").getConfidence());
	}

	@Test
	void buildsCategoryQualifiedTheRetroWebLookupUris() {
		final TheRetroWebId id = new TheRetroWebId(10_510);
		final TheRetroWebReference motherboard = TheRetroWebCategory.MOTHERBOARD.reference(id);
		final TheRetroWebReference expansionCard = TheRetroWebCategory.EXPANSION_CARD.reference(id);

		assertEquals(new TheRetroWebReference(TheRetroWebCategory.MOTHERBOARD, id), motherboard);
		assertEquals(URI.create("https://theretroweb.com/motherboards/10510"), motherboard.lookupUri());
		assertEquals(URI.create("https://theretroweb.com/expansioncards/10510"), expansionCard.lookupUri());
	}
}
