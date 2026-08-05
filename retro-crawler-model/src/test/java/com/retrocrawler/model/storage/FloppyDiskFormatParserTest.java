package com.retrocrawler.model.storage;

import static com.retrocrawler.model.ParserTestContext.CONTEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.gear.Confidence;
import com.retrocrawler.model.storage.FloppyDiskFormat.Density;
import com.retrocrawler.model.storage.FloppyDiskFormat.Sides;

class FloppyDiskFormatParserTest {

	@Test
	void parsesStandaloneSideAndDensityVocabulary() {
		final FloppyDiskFormatParser parser = new FloppyDiskFormatParser();

		assertEquals(FloppyDiskFormat.sides(Sides.SINGLE), parser.parse("SS", CONTEXT).value().orElseThrow());
		assertEquals(FloppyDiskFormat.sides(Sides.DOUBLE), parser.parse("ds", CONTEXT).value().orElseThrow());
		assertEquals(FloppyDiskFormat.density(Density.SINGLE), parser.parse("SD", CONTEXT).value().orElseThrow());
		assertEquals(FloppyDiskFormat.density(Density.DOUBLE), parser.parse("DD", CONTEXT).value().orElseThrow());
		assertEquals(FloppyDiskFormat.density(Density.QUAD), parser.parse("QD", CONTEXT).value().orElseThrow());
		assertEquals(FloppyDiskFormat.density(Density.HIGH), parser.parse("HD", CONTEXT).value().orElseThrow());
		assertEquals(FloppyDiskFormat.density(Density.EXTENDED), parser.parse("ED", CONTEXT).value().orElseThrow());
	}

	@Test
	void parsesConventionalCombinedSpellings() {
		final FloppyDiskFormatParser parser = new FloppyDiskFormatParser();

		assertEquals(FloppyDiskFormat.of(Sides.SINGLE, Density.SINGLE),
				parser.parse("1S-1D", CONTEXT).value().orElseThrow());
		assertEquals(FloppyDiskFormat.of(Sides.SINGLE, Density.DOUBLE),
				parser.parse("1S-2D", CONTEXT).value().orElseThrow());
		assertEquals(FloppyDiskFormat.of(Sides.SINGLE, Density.DOUBLE),
				parser.parse("1S-DD", CONTEXT).value().orElseThrow());
		assertEquals(FloppyDiskFormat.of(Sides.DOUBLE, Density.DOUBLE),
				parser.parse("DS-DD", CONTEXT).value().orElseThrow());
		assertEquals(FloppyDiskFormat.of(Sides.DOUBLE, Density.HIGH),
				parser.parse("2S-HD", CONTEXT).value().orElseThrow());
		assertEquals(FloppyDiskFormat.of(Sides.DOUBLE, Density.QUAD),
				parser.parse("2S-QD", CONTEXT).value().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("2S", CONTEXT).confidence());
		assertEquals(Confidence.NONE, parser.parse("HDMI", CONTEXT).confidence());
	}
}
