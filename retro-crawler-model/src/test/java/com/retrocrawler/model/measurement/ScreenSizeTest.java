package com.retrocrawler.model.measurement;

import static com.retrocrawler.model.ParserTestContext.CONTEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.gear.Confidence;

class ScreenSizeTest {

	@Test
	void parsesUnitQualifiedDisplayDiagonalsAsStrongEvidence() {
		final ScreenSizeParser parser = new ScreenSizeParser();

		assertEquals(new ScreenSize(BigDecimal.valueOf(19)), parser.parse("19″", CONTEXT).value().orElseThrow());
		assertEquals(new ScreenSize(new BigDecimal("14.1")), parser.parse("14,1 inch", CONTEXT).value().orElseThrow());
		assertEquals(Confidence.STRONG, parser.parse("19\"", CONTEXT).confidence());
		assertEquals(Confidence.NONE, parser.parse("19", CONTEXT).confidence());
		assertTrue(new ScreenSize(new BigDecimal("0.4826"), MeasurementUnits.METRE).diagonal()
				.sameLengthAs(new Length(19, MeasurementUnits.INCH)));
		assertThrows(IllegalArgumentException.class, () -> new ScreenSize(BigDecimal.ZERO));
	}
}
