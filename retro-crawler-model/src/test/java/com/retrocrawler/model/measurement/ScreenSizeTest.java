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

		assertEquals(new ScreenSize(BigDecimal.valueOf(19)), parser.parse("19″").value().orElseThrow());
		assertEquals(new ScreenSize(new BigDecimal("14.1")), parser.parse("14,1 inch").value().orElseThrow());
		assertEquals(Confidence.STRONG, parser.parse("19\"").confidence());
		assertEquals(Confidence.NONE, parser.parse("19").confidence());
		assertThrows(IllegalArgumentException.class, () -> new ScreenSize(BigDecimal.ZERO));
	}
}
