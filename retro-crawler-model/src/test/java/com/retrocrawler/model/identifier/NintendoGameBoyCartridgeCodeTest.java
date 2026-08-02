package com.retrocrawler.model.identifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Confidence;

class NintendoGameBoyCartridgeCodeTest {

	private final NintendoGameBoyCartridgeCodeParser parser = new NintendoGameBoyCartridgeCodeParser();

	@Test
	void parsesStandardKnownLabelCodes() {
		assertEquals(new NintendoGameBoyCartridgeCode(
				NintendoGameBoyPlatform.GAME_BOY, "F4", "USA", OptionalInt.of(1)),
				parser.parse("dmg-f4-usa-1").getValue().orElseThrow());
		assertEquals(new NintendoGameBoyCartridgeCode(
				NintendoGameBoyPlatform.GAME_BOY_COLOR, "BMVJ", "JPN"),
				parser.parse("CGB-BMVJ-JPN").getValue().orElseThrow());
		assertEquals(new NintendoGameBoyCartridgeCode(
				NintendoGameBoyPlatform.GAME_BOY_ADVANCE, "A2CP", "EUR"),
				parser.parse("AGB-A2CP-EUR").getValue().orElseThrow());
		assertEquals(new NintendoGameBoyCartridgeCode(
				NintendoGameBoyPlatform.GAME_BOY, "AB", "NNOE"),
				parser.parse("DMG-AB-NNOE").getValue().orElseThrow());

		assertEquals("DMG-F4-USA-1", parser.parse("dmg-f4-usa-1").getValue().orElseThrow().toString());
	}

	@Test
	void preservesObservedLegacyDistributorAndMisprintedForms() {
		final NintendoGameBoyCartridgeCode legacy = parsed("DMG-AWA");
		assertEquals(NintendoGameBoyPlatform.GAME_BOY, legacy.platform().orElseThrow());
		assertEquals("AWA", legacy.gameCode().orElseThrow());
		assertTrue(legacy.distributionCode().isEmpty());

		final NintendoGameBoyCartridgeCode distributor = parsed("DIS-CGB-ADME-USA");
		assertEquals(NintendoGameBoyPlatform.GAME_BOY_COLOR, distributor.platform().orElseThrow());
		assertEquals("ADME", distributor.gameCode().orElseThrow());
		assertEquals("USA", distributor.distributionCode().orElseThrow());

		assertEquals(Confidence.EXACT, parser.parse("AGP-BPPP-EUR").getConfidence());
		assertEquals(Confidence.EXACT, parser.parse("DNG-NE-USA").getConfidence());
		assertEquals(Confidence.EXACT, parser.parse("DMG-401CHN").getConfidence());
		assertTrue(parsed("AGP-BPPP-EUR").platform().isEmpty());

		final NintendoGameBoyCartridgeCode revisedLegacy = parsed("DMG-ZZJ-1");
		assertEquals(OptionalInt.of(1), revisedLegacy.revision());
		assertTrue(revisedLegacy.distributionCode().isEmpty());
	}

	@Test
	void acceptsPlausibleUnknownCodesWithoutPretendingTheyAreCatalogued() {
		assertEquals(Confidence.STRONG, parser.parse("DMG-Z9-XYZ-7").getConfidence());
		assertEquals("DMG-Z9-XYZ-7", parsed("dmg-z9-xyz-7").value());
		assertEquals(Confidence.STRONG, parser.parse("DMG-001").getConfidence());
	}

	@Test
	void rejectsUnrelatedOrMalformedText() {
		assertEquals(Confidence.NONE, parser.parse("NOT-A-CODE").getConfidence());
		assertEquals(Confidence.NONE, parser.parse("DMG F4 USA").getConfidence());
		assertEquals(Confidence.NONE, parser.parse("DMG/F4/USA").getConfidence());
		assertEquals(Confidence.NONE, parser.parse(" ").getConfidence());
		assertEquals(Confidence.NONE, parser.parse(null).getConfidence());
	}

	private NintendoGameBoyCartridgeCode parsed(final String value) {
		return (NintendoGameBoyCartridgeCode) parser.parse(value).getValue().orElseThrow();
	}
}
