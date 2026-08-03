package com.retrocrawler.model.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Confidence;

class StorageFormFactorParserTest {

	@Test
	void parsesCanonicalAndTypographicFloppyFormFactors() {
		final FloppyDiskFormFactorParser parser = new FloppyDiskFormFactorParser();

		assertEquals(FloppyDiskFormFactor.INCH_3_5, parser.parse("3,5\"").value().orElseThrow());
		assertEquals(FloppyDiskFormFactor.INCH_3_5, parser.parse("3½″").value().orElseThrow());
		assertEquals(FloppyDiskFormFactor.INCH_5_25, parser.parse("5.25 inch").value().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("3,5").confidence());
		assertEquals(Confidence.NONE, parser.parse("2,5″").confidence());
		assertEquals(Confidence.NONE, parser.parse("19″").confidence());
	}

	@Test
	void parsesPortableHardDiskDriveFormFactors() {
		final HardDiskDriveFormFactorParser parser = new HardDiskDriveFormFactorParser();

		assertEquals(HardDiskDriveFormFactor.INCH_1_8, parser.parse("1.8\"").value().orElseThrow());
		assertEquals(HardDiskDriveFormFactor.INCH_2_5, parser.parse("2,5″").value().orElseThrow());
		assertEquals(HardDiskDriveFormFactor.INCH_3_5, parser.parse("3.5 inches").value().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("2,5").confidence());
	}
}
