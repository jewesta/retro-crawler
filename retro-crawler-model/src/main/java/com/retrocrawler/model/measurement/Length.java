package com.retrocrawler.model.measurement;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Objects;

/** A positive one-dimensional physical measurement. */
public record Length(BigDecimal amount, Unit unit) {

	public Length {
		Objects.requireNonNull(amount, "amount");
		Objects.requireNonNull(unit, "unit");
		if (amount.signum() <= 0) {
			throw new IllegalArgumentException("Length must be positive: " + amount);
		}
		amount = amount.stripTrailingZeros();
	}

	public BigDecimal inMillimeters() {
		return amount.multiply(unit.millimetersPerUnit()).stripTrailingZeros();
	}

	public BigDecimal amountIn(final Unit targetUnit) {
		Objects.requireNonNull(targetUnit, "targetUnit");
		return inMillimeters().divide(targetUnit.millimetersPerUnit(), MathContext.DECIMAL128).stripTrailingZeros();
	}

	public boolean sameLengthAs(final Length other) {
		Objects.requireNonNull(other, "other");
		return inMillimeters().compareTo(other.inMillimeters()) == 0;
	}

	@Override
	public String toString() {
		return amount.toPlainString() + unit.symbol();
	}

	public enum Unit {

		MILLIMETER("mm", "1"),
		CENTIMETER("cm", "10"),
		METER("m", "1000"),
		INCH("\"", "25.4");

		private final String symbol;
		private final BigDecimal millimetersPerUnit;

		Unit(final String symbol, final String millimetersPerUnit) {
			this.symbol = symbol;
			this.millimetersPerUnit = new BigDecimal(millimetersPerUnit);
		}

		public String symbol() {
			return symbol;
		}

		BigDecimal millimetersPerUnit() {
			return millimetersPerUnit;
		}
	}
}
