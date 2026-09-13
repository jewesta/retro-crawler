package com.retrocrawler.model.hardware;

import javax.measure.Unit;
import javax.measure.quantity.Time;

import com.retrocrawler.model.measurement.MeasurementUnits;
import com.retrocrawler.model.measurement.PositiveQuantity;

/** A positive memory access duration. */
public final class MemoryAccessTime extends PositiveQuantity<Time> {

	private static final long serialVersionUID = 1L;

	public MemoryAccessTime(final int nanoseconds) {
		this(nanoseconds, MeasurementUnits.NANOSECOND);
	}

	public MemoryAccessTime(final Number amount, final Unit<Time> unit) {
		super(amount, unit);
	}

	public int nanoseconds() {
		return amountIn(MeasurementUnits.NANOSECOND).intValueExact();
	}

	@Override
	public String toString() {
		return amount().toPlainString() + (MeasurementUnits.NANOSECOND.equals(unit()) ? "ns" : " " + unit());
	}
}
