package com.retrocrawler.model.appearance;

import static com.retrocrawler.model.ParserTestContext.CONTEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Confidence;

class ColorParserTest {

	private final ColorParser parser = new ColorParser();

	@Test
	void parsesCanonicalNamedColorsAndEnglishAliases() {
		assertEquals(Color.BLACK, parser.parse("black", CONTEXT).value().orElseThrow());
		assertEquals(Color.GRAY, parser.parse("grey", CONTEXT).value().orElseThrow());
		assertEquals(Color.MULTICOLORED, parser.parse("multi-coloured", CONTEXT).value().orElseThrow());
	}

	@Test
	void rejectsCompoundColorObservations() {
		assertEquals(Confidence.NONE, parser.parse("white/pink", CONTEXT).confidence());
		assertEquals(Confidence.NONE, parser.parse("white-pink", CONTEXT).confidence());
	}

	@Test
	void suppliesStableUiColorCodesWhereOneColorCanBeRendered() {
		assertEquals(Optional.of("#FFFFFF"), Color.WHITE.colorCode());
		assertEquals(Optional.empty(), Color.MULTICOLORED.colorCode());
	}

	@Test
	void doesNotPretendThatTransparencyOrCollectionLanguageIsAColor() {
		assertEquals(Confidence.NONE, parser.parse("transparent", CONTEXT).confidence());
		assertEquals(Confidence.NONE, parser.parse("weiß", CONTEXT).confidence());
		assertEquals(Confidence.NONE, parser.parse(null, CONTEXT).confidence());
	}
}
