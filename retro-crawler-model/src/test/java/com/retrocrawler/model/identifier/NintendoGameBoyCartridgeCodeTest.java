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
		assertEquals(new NintendoGameBoyCartridgeCode(NintendoGameBoyPlatform.GAME_BOY, "F4", "USA", OptionalInt.of(1)),
				parser.parse("dmg-f4-usa-1").value().orElseThrow());
		assertEquals(new NintendoGameBoyCartridgeCode(NintendoGameBoyPlatform.GAME_BOY_COLOR, "BMVJ", "JPN"),
				parser.parse("CGB-BMVJ-JPN").value().orElseThrow());
		assertEquals(new NintendoGameBoyCartridgeCode(NintendoGameBoyPlatform.GAME_BOY_ADVANCE, "A2CP", "EUR"),
				parser.parse("AGB-A2CP-EUR").value().orElseThrow());
		assertEquals(new NintendoGameBoyCartridgeCode(NintendoGameBoyPlatform.GAME_BOY, "AB", "NNOE"),
				parser.parse("DMG-AB-NNOE").value().orElseThrow());

		assertEquals("DMG-F4-USA-1", parser.parse("dmg-f4-usa-1").value().orElseThrow().toString());
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

		assertEquals(Confidence.EXACT, parser.parse("AGP-BPPP-EUR").confidence());
		assertEquals(Confidence.EXACT, parser.parse("DNG-NE-USA").confidence());
		assertEquals(Confidence.EXACT, parser.parse("DMG-401CHN").confidence());
		assertTrue(parsed("AGP-BPPP-EUR").platform().isEmpty());

		final NintendoGameBoyCartridgeCode revisedLegacy = parsed("DMG-ZZJ-1");
		assertEquals(OptionalInt.of(1), revisedLegacy.revision());
		assertTrue(revisedLegacy.distributionCode().isEmpty());
	}

	@Test
	void acceptsPlausibleUnknownCodesWithoutPretendingTheyAreCatalogued() {
		assertEquals(Confidence.STRONG, parser.parse("DMG-Z9-XYZ-7").confidence());
		assertEquals("DMG-Z9-XYZ-7", parsed("dmg-z9-xyz-7").value());
		assertEquals(Confidence.STRONG, parser.parse("DMG-001").confidence());
	}

	@Test
	void rejectsUnrelatedOrMalformedText() {
		assertEquals(Confidence.NONE, parser.parse("NOT-A-CODE").confidence());
		assertEquals(Confidence.NONE, parser.parse("DMG F4 USA").confidence());
		assertEquals(Confidence.NONE, parser.parse("DMG/F4/USA").confidence());
		assertEquals(Confidence.NONE, parser.parse(" ").confidence());
		assertEquals(Confidence.NONE, parser.parse(null).confidence());
	}

	private NintendoGameBoyCartridgeCode parsed(final String value) {
		return parser.parse(value).value().orElseThrow();
	}
}
