package com.retrocrawler.model.measurement;

import static javax.measure.MetricPrefix.MEGA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static tech.units.indriya.unit.Units.HERTZ;

import java.math.BigDecimal;

import javax.measure.Quantity;
import javax.measure.quantity.Frequency;
import javax.measure.quantity.Time;

import org.junit.jupiter.api.Test;

import com.retrocrawler.model.hardware.MemoryAccessTime;

import tech.units.indriya.quantity.Quantities;

class QuantityFoundationTest {

	@Test
	void representsAndConvertsAStandardQuantityWithoutLosingDecimalPrecision() {
		final Quantity<Frequency> frequency = Quantities.getQuantity(new BigDecimal("4.77"), MEGA(HERTZ));

		assertEquals(new BigDecimal("4770000"), new BigDecimal(frequency.to(HERTZ).getValue().toString()));
	}

	@Test
	void modelsExistingMeasurementsAsTypedQuantities() {
		final Quantity<javax.measure.quantity.Length> length = new Length(2.5, MeasurementUnits.INCH);
		final Quantity<javax.measure.quantity.Length> screenSize = new ScreenSize(19);
		final Quantity<javax.measure.quantity.Power> power = new Power(300);
		final Quantity<Time> memoryAccessTime = new MemoryAccessTime(70);
		final Quantity<DataCapacity> capacity = new DataCapacity(512, MeasurementUnits.MEBIBYTE);
		final Quantity<TrackDensity> trackDensity = new TrackDensity(96);

		assertEquals(new BigDecimal("63.5"),
				new BigDecimal(length.to(MeasurementUnits.MILLIMETRE).getValue().toString()).stripTrailingZeros());
		assertEquals(19, screenSize.getValue().intValue());
		assertEquals(300, power.getValue().intValue());
		assertEquals(70, memoryAccessTime.getValue().intValue());
		assertEquals(512, capacity.getValue().intValue());
		assertEquals(96, trackDensity.getValue().intValue());
		assertNotEquals(capacity.getUnit(), trackDensity.getUnit());
		final Quantity<javax.measure.quantity.Length> lengthInMetres = new Length(2.5, MeasurementUnits.METRE);
		final Number sumInMetres = lengthInMetres.add(new Length(0.5, MeasurementUnits.METRE)).getValue();
		assertEquals(new BigDecimal("3"), new BigDecimal(sumInMetres.toString()).stripTrailingZeros());
	}
}
