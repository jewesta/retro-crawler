package com.retrocrawler.model.locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Confidence;

class LocaleCodeParsersTest {

	@Test
	void parsesStandardLanguageCodesAndEnglishNames() {
		final LanguageCodeParser parser = new LanguageCodeParser();

		assertEquals(new LanguageCode("de"), parser.parse("DE").getValue().orElseThrow());
		assertEquals(new LanguageCode("de"), parser.parse("German").getValue().orElseThrow());
		assertEquals(new LanguageCode("en"), parser.parse("en").getValue().orElseThrow());
		assertEquals(new LanguageCode("eu"), parser.parse("EU").getValue().orElseThrow());
		assertThrows(IllegalArgumentException.class, () -> new LanguageCode("not-a-language"));
	}

	@Test
	void parsesCountryAndIndustryReleaseRegions() {
		final RegionCodeParser parser = new RegionCodeParser();

		assertEquals(new RegionCode("US"), parser.parse("us").getValue().orElseThrow());
		assertEquals(new RegionCode("JP"), parser.parse("JP").getValue().orElseThrow());
		assertEquals(new RegionCode("EUR"), parser.parse("eur").getValue().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("EU").getConfidence());
		assertEquals(Confidence.NONE, parser.parse("EN").getConfidence());
		assertThrows(IllegalArgumentException.class, () -> new RegionCode("XX"));
	}

	@Test
	void exposesCodesThatAreIntrinsicallyAmbiguousWithoutChoosingAMeaning() {
		final LanguageCodeParser language = new LanguageCodeParser();
		final RegionCodeParser region = new RegionCodeParser();

		for (final String code : new String[] { "DE", "ES", "FR", "IT" }) {
			assertEquals(Confidence.EXACT, language.parse(code).getConfidence());
			assertEquals(Confidence.EXACT, region.parse(code).getConfidence());
		}

		assertEquals(Confidence.EXACT, language.parse("EU").getConfidence());
		assertEquals(Confidence.NONE, region.parse("EU").getConfidence());
	}
}
