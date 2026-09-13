package com.retrocrawler.model.measurement;

import java.math.BigDecimal;

import javax.measure.Unit;

/** The conventionally quoted diagonal size of a display, measured in inches. */
public final class ScreenSize extends PositiveQuantity<javax.measure.quantity.Length> {

	private static final long serialVersionUID = 1L;

	public ScreenSize(final Number diagonalInches) {
		this(diagonalInches, MeasurementUnits.INCH);
	}

	public ScreenSize(final Number amount, final Unit<javax.measure.quantity.Length> unit) {
		super(amount, unit);
	}

	public BigDecimal diagonalInches() {
		return amountIn(MeasurementUnits.INCH);
	}

	@Override
	public String toString() {
		return diagonalInches().toPlainString() + '"';
	}

	public Length diagonal() {
		return new Length(amount(), unit());
	}
}
