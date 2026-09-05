package com.retrocrawler.model.measurement;

import static javax.measure.MetricPrefix.MEGA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static tech.units.indriya.unit.Units.HERTZ;

import java.math.BigDecimal;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.quantity.Frequency;

import org.junit.jupiter.api.Test;

import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.BaseUnit;

class QuantityFoundationTest {

	private static final Unit<TerminalCount> TERMINAL = new BaseUnit<>("terminal");
	private static final Unit<LaneCount> LANE = new BaseUnit<>("lane");

	@Test
	void representsAndConvertsAStandardQuantityWithoutLosingDecimalPrecision() {
		final Quantity<Frequency> frequency = Quantities.getQuantity(new BigDecimal("4.77"), MEGA(HERTZ));

		assertEquals(new BigDecimal("4770000"), new BigDecimal(frequency.to(HERTZ).getValue().toString()));
	}

	@Test
	void keepsSemanticallyDifferentCustomCountsInDifferentQuantityTypes() {
		final Quantity<TerminalCount> terminals = Quantities.getQuantity(68, TERMINAL);
		final Quantity<LaneCount> lanes = Quantities.getQuantity(68, LANE);

		assertEquals(68, terminals.getValue().intValue());
		assertEquals(68, lanes.getValue().intValue());
		assertNotEquals(TERMINAL, LANE);
	}

	private interface TerminalCount extends Quantity<TerminalCount> {
	}

	private interface LaneCount extends Quantity<LaneCount> {
	}
}
