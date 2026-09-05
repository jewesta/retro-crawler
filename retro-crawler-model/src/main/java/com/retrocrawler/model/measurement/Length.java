package com.retrocrawler.model.measurement;

import java.math.BigDecimal;
import java.util.Objects;

import javax.measure.Unit;

/** A positive one-dimensional physical measurement. */
public final class Length extends PositiveQuantity<javax.measure.quantity.Length> {

	private static final long serialVersionUID = 1L;

	public Length(final Number amount, final Unit<javax.measure.quantity.Length> unit) {
		super(amount, unit);
	}

	public BigDecimal inMillimeters() {
		return amountIn(MeasurementUnits.MILLIMETRE);
	}

	public boolean sameLengthAs(final Length other) {
		return isEquivalentTo(Objects.requireNonNull(other, "other"));
	}

	@Override
	public String toString() {
		return amount().toPlainString() + symbol(unit());
	}

	private static String symbol(final Unit<javax.measure.quantity.Length> unit) {
		if (MeasurementUnits.MILLIMETRE.equals(unit)) {
			return "mm";
		}
		if (MeasurementUnits.CENTIMETRE.equals(unit)) {
			return "cm";
		}
		if (MeasurementUnits.METRE.equals(unit)) {
			return "m";
		}
		if (MeasurementUnits.INCH.equals(unit)) {
			return "\"";
		}
		return " " + unit;
	}
}
