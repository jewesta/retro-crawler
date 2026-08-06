package com.retrocrawler.model.storage;

import static com.retrocrawler.model.ParserTestContext.CONTEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.gear.Confidence;

class StorageFormFactorParserTest {

	@Test
	void parsesCanonicalAndTypographicFloppyFormFactors() {
		final FloppyDiskFormFactorParser parser = new FloppyDiskFormFactorParser();

		assertEquals(FloppyDiskFormFactor.INCH_3_5, parser.parse("3,5\"", CONTEXT).value().orElseThrow());
		assertEquals(FloppyDiskFormFactor.INCH_3_5, parser.parse("3½″", CONTEXT).value().orElseThrow());
		assertEquals(FloppyDiskFormFactor.INCH_5_25, parser.parse("5.25 inch", CONTEXT).value().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("3,5", CONTEXT).confidence());
		assertEquals(Confidence.NONE, parser.parse("2,5″", CONTEXT).confidence());
		assertEquals(Confidence.NONE, parser.parse("19″", CONTEXT).confidence());
	}

	@Test
	void parsesPortableHardDiskDriveFormFactors() {
		final HardDiskDriveFormFactorParser parser = new HardDiskDriveFormFactorParser();

		assertEquals(HardDiskDriveFormFactor.INCH_1_8, parser.parse("1.8\"", CONTEXT).value().orElseThrow());
		assertEquals(HardDiskDriveFormFactor.INCH_2_5, parser.parse("2,5″", CONTEXT).value().orElseThrow());
		assertEquals(HardDiskDriveFormFactor.INCH_3_5, parser.parse("3.5 inches", CONTEXT).value().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("2,5", CONTEXT).confidence());
	}
}
