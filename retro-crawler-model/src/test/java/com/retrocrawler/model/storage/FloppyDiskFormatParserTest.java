package com.retrocrawler.model.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Confidence;
import com.retrocrawler.model.storage.FloppyDiskFormat.Density;
import com.retrocrawler.model.storage.FloppyDiskFormat.Sides;

class FloppyDiskFormatParserTest {

	@Test
	void parsesStandaloneSideAndDensityVocabulary() {
		final FloppyDiskFormatParser parser = new FloppyDiskFormatParser();

		assertEquals(FloppyDiskFormat.sides(Sides.SINGLE), parser.parse("SS").getValue().orElseThrow());
		assertEquals(FloppyDiskFormat.sides(Sides.DOUBLE), parser.parse("ds").getValue().orElseThrow());
		assertEquals(FloppyDiskFormat.density(Density.SINGLE), parser.parse("SD").getValue().orElseThrow());
		assertEquals(FloppyDiskFormat.density(Density.DOUBLE), parser.parse("DD").getValue().orElseThrow());
		assertEquals(FloppyDiskFormat.density(Density.QUAD), parser.parse("QD").getValue().orElseThrow());
		assertEquals(FloppyDiskFormat.density(Density.HIGH), parser.parse("HD").getValue().orElseThrow());
		assertEquals(FloppyDiskFormat.density(Density.EXTENDED), parser.parse("ED").getValue().orElseThrow());
	}

	@Test
	void parsesConventionalCombinedSpellings() {
		final FloppyDiskFormatParser parser = new FloppyDiskFormatParser();

		assertEquals(FloppyDiskFormat.of(Sides.SINGLE, Density.SINGLE),
				parser.parse("1S-1D").getValue().orElseThrow());
		assertEquals(FloppyDiskFormat.of(Sides.SINGLE, Density.DOUBLE),
				parser.parse("1S-2D").getValue().orElseThrow());
		assertEquals(FloppyDiskFormat.of(Sides.SINGLE, Density.DOUBLE),
				parser.parse("1S-DD").getValue().orElseThrow());
		assertEquals(FloppyDiskFormat.of(Sides.DOUBLE, Density.DOUBLE),
				parser.parse("DS-DD").getValue().orElseThrow());
		assertEquals(FloppyDiskFormat.of(Sides.DOUBLE, Density.HIGH),
				parser.parse("2S-HD").getValue().orElseThrow());
		assertEquals(FloppyDiskFormat.of(Sides.DOUBLE, Density.QUAD),
				parser.parse("2S-QD").getValue().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("2S").getConfidence());
		assertEquals(Confidence.NONE, parser.parse("HDMI").getConfidence());
	}
}
