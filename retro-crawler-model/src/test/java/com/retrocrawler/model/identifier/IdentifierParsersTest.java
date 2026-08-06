package com.retrocrawler.model.identifier;

import static com.retrocrawler.model.ParserTestContext.CONTEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.gear.Confidence;

import de.creativecouple.validation.isbn.ISBN;

class IdentifierParsersTest {

	@Test
	void parsesSegaGameGearCartridgeCatalogueNumbers() {
		final SegaGameGearCartridgeCodeParser parser = new SegaGameGearCartridgeCodeParser();

		assertEquals(new SegaGameGearCartridgeCode("2449"), parser.parse("2449", CONTEXT).value().orElseThrow());
		assertEquals(new SegaGameGearCartridgeCode("2567"), parser.parse("GG-2567", CONTEXT).value().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("GG 256", CONTEXT).confidence());
	}

	@Test
	void validatesAndNormalizesIsbn10AndIsbn13() {
		final ISBNParser parser = new ISBNParser();
		final ISBN isbn10 = parser.parse("0-306-40615-2", CONTEXT).value().orElseThrow();
		final ISBN isbn13 = parser.parse("978-0-306-40615-7", CONTEXT).value().orElseThrow();

		assertEquals(isbn13, isbn10);
		assertEquals("9780306406157", isbn13.toCompactString());
		assertEquals("978-0-306-40615-7", isbn13.toString());
		assertEquals("0", isbn13.getGroup());
		assertEquals("English language", isbn13.getGroupName());
		assertEquals("306", isbn13.getPublisher());
		assertEquals("40615", isbn13.getTitle());
		assertEquals(URI.create("urn:isbn:9780306406157"), isbn13.toURI());
		assertEquals(Confidence.NONE, parser.parse("978-0-306-40615-8", CONTEXT).confidence());
		assertEquals(Confidence.NONE, parser.parse("not-an-isbn", CONTEXT).confidence());
	}

	@Test
	void normalizesCommonMacAddressNotations() {
		final MacAddressParser parser = new MacAddressParser();
		final MacAddress expected = new MacAddress("00:00:C0:0D:66:AB");

		assertEquals(expected, parser.parse("00-00-c0-0d-66-ab", CONTEXT).value().orElseThrow());
		assertEquals(expected, parser.parse("0000.c00d.66ab", CONTEXT).value().orElseThrow());
		assertEquals("0000C00D66AB", expected.toCompactString());
		assertEquals(Confidence.NONE, parser.parse("00:00:C0:0D:66", CONTEXT).confidence());
	}

	@Test
	void parsesTheRetroWebIdsAsReusableExternalReferences() {
		final TheRetroWebIdParser parser = new TheRetroWebIdParser();

		assertEquals(new TheRetroWebId(10_510), parser.parse(" 10510 ", CONTEXT).value().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("motherboard-10510", CONTEXT).confidence());
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
