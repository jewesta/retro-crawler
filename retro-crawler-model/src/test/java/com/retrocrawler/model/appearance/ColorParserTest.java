package com.retrocrawler.model.appearance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Confidence;

class ColorParserTest {

	private final ColorParser parser = new ColorParser();

	@Test
	void parsesCanonicalNamedColorsAndEnglishAliases() {
		assertEquals(Color.BLACK, parser.parse("black").getValue().orElseThrow());
		assertEquals(Color.GRAY, parser.parse("grey").getValue().orElseThrow());
		assertEquals(Color.MULTICOLORED, parser.parse("multi-coloured").getValue().orElseThrow());
		assertEquals(Set.of(Color.WHITE, Color.PINK), parser.parse("white/pink").getValue().orElseThrow());
		assertEquals(Set.of(Color.WHITE, Color.PINK), parser.parse("white-pink").getValue().orElseThrow());
	}

	@Test
	void suppliesStableUiColorCodesWhereOneColorCanBeRendered() {
		assertEquals(Optional.of("#FFFFFF"), Color.WHITE.getColorCode());
		assertEquals(Optional.empty(), Color.MULTICOLORED.getColorCode());
	}

	@Test
	void doesNotPretendThatTransparencyOrCollectionLanguageIsAColor() {
		assertEquals(Confidence.NONE, parser.parse("transparent").getConfidence());
		assertEquals(Confidence.NONE, parser.parse("weiß").getConfidence());
		assertEquals(Confidence.NONE, parser.parse(null).getConfidence());
	}
}
