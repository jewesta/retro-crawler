package com.retrocrawler.model.locale;

import static com.retrocrawler.model.ParserTestContext.CONTEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Confidence;

class LocaleCodeParsersTest {

	@Test
	void parsesStandardLanguageCodesAndEnglishNames() {
		final LanguageCodeParser parser = new LanguageCodeParser();

		assertEquals(new LanguageCode("de"), parser.parse("DE", CONTEXT).value().orElseThrow());
		assertEquals(new LanguageCode("de"), parser.parse("German", CONTEXT).value().orElseThrow());
		assertEquals(new LanguageCode("en"), parser.parse("en", CONTEXT).value().orElseThrow());
		assertEquals(new LanguageCode("eu"), parser.parse("EU", CONTEXT).value().orElseThrow());
		assertThrows(IllegalArgumentException.class, () -> new LanguageCode("not-a-language"));
	}

	@Test
	void parsesCountryAndIndustryReleaseRegions() {
		final RegionCodeParser parser = new RegionCodeParser();

		assertEquals(new RegionCode("US"), parser.parse("us", CONTEXT).value().orElseThrow());
		assertEquals(new RegionCode("JP"), parser.parse("JP", CONTEXT).value().orElseThrow());
		assertEquals(new RegionCode("EUR"), parser.parse("eur", CONTEXT).value().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("EU", CONTEXT).confidence());
		assertEquals(Confidence.NONE, parser.parse("EN", CONTEXT).confidence());
		assertThrows(IllegalArgumentException.class, () -> new RegionCode("XX"));
	}

	@Test
	void exposesCodesThatAreIntrinsicallyAmbiguousWithoutChoosingAMeaning() {
		final LanguageCodeParser language = new LanguageCodeParser();
		final RegionCodeParser region = new RegionCodeParser();

		for (final String code : new String[] {
				"DE", "ES", "FR", "IT"
		}) {
			assertEquals(Confidence.EXACT, language.parse(code, CONTEXT).confidence());
			assertEquals(Confidence.EXACT, region.parse(code, CONTEXT).confidence());
		}

		assertEquals(Confidence.EXACT, language.parse("EU", CONTEXT).confidence());
		assertEquals(Confidence.NONE, region.parse("EU", CONTEXT).confidence());
	}
}
