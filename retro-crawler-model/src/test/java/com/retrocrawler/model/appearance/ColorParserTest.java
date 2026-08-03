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
		assertEquals(Color.BLACK, parser.parse("black").value().orElseThrow());
		assertEquals(Color.GRAY, parser.parse("grey").value().orElseThrow());
		assertEquals(Color.MULTICOLORED, parser.parse("multi-coloured").value().orElseThrow());
		assertEquals(Set.of(Color.WHITE, Color.PINK), parser.parse("white/pink").value().orElseThrow());
		assertEquals(Set.of(Color.WHITE, Color.PINK), parser.parse("white-pink").value().orElseThrow());
	}

	@Test
	void suppliesStableUiColorCodesWhereOneColorCanBeRendered() {
		assertEquals(Optional.of("#FFFFFF"), Color.WHITE.colorCode());
		assertEquals(Optional.empty(), Color.MULTICOLORED.colorCode());
	}

	@Test
	void doesNotPretendThatTransparencyOrCollectionLanguageIsAColor() {
		assertEquals(Confidence.NONE, parser.parse("transparent").confidence());
		assertEquals(Confidence.NONE, parser.parse("weiß").confidence());
		assertEquals(Confidence.NONE, parser.parse(null).confidence());
	}
}
