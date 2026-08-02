package com.retrocrawler.model.measurement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Confidence;

class ScreenSizeTest {

	@Test
	void parsesUnitQualifiedDisplayDiagonalsAsStrongEvidence() {
		final ScreenSizeParser parser = new ScreenSizeParser();

		assertEquals(new ScreenSize(BigDecimal.valueOf(19)), parser.parse("19″").getValue().orElseThrow());
		assertEquals(new ScreenSize(new BigDecimal("14.1")), parser.parse("14,1 inch").getValue().orElseThrow());
		assertEquals(Confidence.STRONG, parser.parse("19\"").getConfidence());
		assertEquals(Confidence.NONE, parser.parse("19").getConfidence());
		assertThrows(IllegalArgumentException.class, () -> new ScreenSize(BigDecimal.ZERO));
	}
}
