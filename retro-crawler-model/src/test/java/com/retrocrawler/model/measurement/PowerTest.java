package com.retrocrawler.model.measurement;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Confidence;

class PowerTest {

	@Test
	void parsesElectricalPowerWithoutConfusingOtherMeasurements() {
		final PowerParser parser = new PowerParser();

		assertEquals(new Power(BigDecimal.valueOf(400)), parser.parse("400W").value().orElseThrow());
		assertEquals(new Power(new BigDecimal("3.3")), parser.parse("3,3 W").value().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("400MB").confidence());
	}
}
