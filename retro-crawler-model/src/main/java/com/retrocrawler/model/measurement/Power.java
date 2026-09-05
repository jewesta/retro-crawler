package com.retrocrawler.model.measurement;

import java.math.BigDecimal;

import javax.measure.Unit;

/** A positive physical power measurement. */
public final class Power extends PositiveQuantity<javax.measure.quantity.Power> {

	private static final long serialVersionUID = 1L;

	public Power(final Number watts) {
		this(watts, MeasurementUnits.WATT);
	}

	public Power(final Number amount, final Unit<javax.measure.quantity.Power> unit) {
		super(amount, unit);
	}

	public BigDecimal watts() {
		return amountIn(MeasurementUnits.WATT);
	}

	@Override
	public String toString() {
		return amount().toPlainString() + (MeasurementUnits.WATT.equals(unit()) ? "W" : " " + unit());
	}
}
