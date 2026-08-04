package com.retrocrawler.model.identifier;

import static com.retrocrawler.model.ParserTestContext.CONTEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Confidence;
import com.retrocrawler.model.locale.LanguageCode;
import com.retrocrawler.model.locale.RegionCode;

class NintendoGameBoyCartridgeCatalogTest {

	private final NintendoGameBoyCartridgeCatalog catalog = NintendoGameBoyCartridgeCatalog.bundled();

	@Test
	void loadsTheDatedResourceSnapshot() {
		assertEquals(626, catalog.entries().size());
		assertEquals(611, catalog.codes().size());
		assertTrue(catalog.entries().stream().allMatch(entry -> !entry.releaseRegions().isEmpty()));
		assertEquals(117, catalog.entries().stream().filter(entry -> !entry.gameLanguageSets().isEmpty()).count());
		assertEquals(3, catalog.entries().stream().filter(entry -> entry.gameLanguageSets().size() > 1).count());
		assertTrue(catalog.contains(new NintendoGameBoyCartridgeCode("DIS-CGB-ADME-USA")));
		assertTrue(catalog.contains(new NintendoGameBoyCartridgeCode("AGP-BPPP-EUR")));
		assertTrue(catalog.contains(new NintendoGameBoyCartridgeCode("DNG-NE-USA")));

		final NintendoGameBoyCartridgeCodeParser parser = new NintendoGameBoyCartridgeCodeParser(catalog);
		for (final NintendoGameBoyCartridgeCode code : catalog.codes()) {
			assertEquals(Confidence.EXACT, parser.parse(code.value(), CONTEXT).confidence(), code::value);
		}
	}

	@Test
	void keepsOneToManyLabelCodeMappings() {
		final List<NintendoGameBoyCartridgeCatalogEntry> releases = catalog
				.findByCode(new NintendoGameBoyCartridgeCode("AGB-AXPE-USA"));
		assertEquals(3, releases.size());
		assertEquals(List.of(0, 1, 2), releases.stream().map(entry -> entry.romId().revision()).toList());
	}

	@Test
	void supportsExactReverseTitleAndRomLookups() {
		final NintendoGameBoyRomId romId = NintendoGameBoyRomId.parse("DMG-F4E-0").orElseThrow();
		assertEquals("DMG-F4-USA-1", catalog.findByRomId(romId).getFirst().cartridgeCode().value());
		assertFalse(catalog.findByTitle("4-IN-1 FUN PAK (USA, EUROPE)").isEmpty());
		assertTrue(catalog.findByCode(new NintendoGameBoyCartridgeCode("DMG-Z9-XYZ-7")).isEmpty());
	}

	@Test
	void modelsReleaseRegionsAndExplicitGameLanguages() {
		final NintendoGameBoyCartridgeCatalogEntry castlevania = catalog
				.findByCode(new NintendoGameBoyCartridgeCode("AGB-A2CP-EUR")).getFirst();
		assertEquals(Set.of(new RegionCode("EUR")), castlevania.releaseRegions());
		assertEquals(List.of(Set.of(new LanguageCode("en"), new LanguageCode("fr"), new LanguageCode("de"))),
				castlevania.gameLanguageSets());

		final NintendoGameBoyCartridgeCatalogEntry worldRelease = catalog
				.findByCode(new NintendoGameBoyCartridgeCode("DMG-MLA")).getFirst();
		assertEquals(Set.of(new RegionCode("JP"), new RegionCode("US"), new RegionCode("EUR")),
				worldRelease.releaseRegions());
		assertTrue(worldRelease.gameLanguageSets().isEmpty());

		assertTrue(catalog.findByReleaseRegion(new RegionCode("EUR")).contains(castlevania));
		assertTrue(catalog.findByGameLanguage(new LanguageCode("de")).contains(castlevania));
	}

	@Test
	void preservesSeparateLanguageSetsForCompilationCartridges() {
		final NintendoGameBoyCartridgeCatalogEntry compilation = catalog
				.findByCode(new NintendoGameBoyCartridgeCode("AGB-B2BP-EUR")).getFirst();

		assertEquals(2, compilation.gameLanguageSets().size());
		assertEquals(Set.of(new LanguageCode("en"), new LanguageCode("fr"), new LanguageCode("de"),
				new LanguageCode("es"), new LanguageCode("it"), new LanguageCode("nl")),
				compilation.gameLanguageSets().get(0));
		assertEquals(Set.of(new LanguageCode("en"), new LanguageCode("fr"), new LanguageCode("de"),
				new LanguageCode("es"), new LanguageCode("nl")), compilation.gameLanguageSets().get(1));
	}

	@Test
	void readsExternalCataloguesAndValidatesTheirSchema() {
		final String data = """
			# source metadata
			cartridge_code\trom_id\ttitle\trelease_regions\tgame_languages
			DMG-F4-USA-1\tDMG-F4E-0\t4-in-1 Fun Pak (USA, Europe)\tUS,EUR\t
			""";
		final NintendoGameBoyCartridgeCatalog external = NintendoGameBoyCartridgeCatalog.read(new StringReader(data));
		assertEquals(1, external.entries().size());
		assertEquals(626, catalog.plus(external).entries().size());
		assertThrows(UnsupportedOperationException.class, () -> external.entries().add(external.entries().getFirst()));

		assertThrows(IllegalArgumentException.class,
				() -> NintendoGameBoyCartridgeCatalog.read(new StringReader("wrong\theader\n")));
	}

	@Test
	void modelsRomIdsSeparatelyFromPrintedLabelCodes() {
		final NintendoGameBoyRomId romId = NintendoGameBoyRomId.parse("dmg-f4e-0").orElseThrow();
		assertEquals(NintendoGameBoyPlatform.GAME_BOY, romId.platform());
		assertEquals("F4E", romId.gameCode());
		assertEquals(0, romId.revision());
		assertEquals("DMG-F4E-0", romId.toString());
		assertTrue(NintendoGameBoyRomId.parse("DMG-F4-USA-1").isEmpty());
	}
}
