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

		assertEquals(FloppyDiskFormat.sides(Sides.SINGLE), parser.parse("SS").value().orElseThrow());
		assertEquals(FloppyDiskFormat.sides(Sides.DOUBLE), parser.parse("ds").value().orElseThrow());
		assertEquals(FloppyDiskFormat.density(Density.SINGLE), parser.parse("SD").value().orElseThrow());
		assertEquals(FloppyDiskFormat.density(Density.DOUBLE), parser.parse("DD").value().orElseThrow());
		assertEquals(FloppyDiskFormat.density(Density.QUAD), parser.parse("QD").value().orElseThrow());
		assertEquals(FloppyDiskFormat.density(Density.HIGH), parser.parse("HD").value().orElseThrow());
		assertEquals(FloppyDiskFormat.density(Density.EXTENDED), parser.parse("ED").value().orElseThrow());
	}

	@Test
	void parsesConventionalCombinedSpellings() {
		final FloppyDiskFormatParser parser = new FloppyDiskFormatParser();

		assertEquals(FloppyDiskFormat.of(Sides.SINGLE, Density.SINGLE),
				parser.parse("1S-1D").value().orElseThrow());
		assertEquals(FloppyDiskFormat.of(Sides.SINGLE, Density.DOUBLE),
				parser.parse("1S-2D").value().orElseThrow());
		assertEquals(FloppyDiskFormat.of(Sides.SINGLE, Density.DOUBLE),
				parser.parse("1S-DD").value().orElseThrow());
		assertEquals(FloppyDiskFormat.of(Sides.DOUBLE, Density.DOUBLE),
				parser.parse("DS-DD").value().orElseThrow());
		assertEquals(FloppyDiskFormat.of(Sides.DOUBLE, Density.HIGH),
				parser.parse("2S-HD").value().orElseThrow());
		assertEquals(FloppyDiskFormat.of(Sides.DOUBLE, Density.QUAD),
				parser.parse("2S-QD").value().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("2S").confidence());
		assertEquals(Confidence.NONE, parser.parse("HDMI").confidence());
	}
}
