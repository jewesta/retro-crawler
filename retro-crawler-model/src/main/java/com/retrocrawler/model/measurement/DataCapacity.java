package com.retrocrawler.model.measurement;

import java.math.BigDecimal;
import java.util.Objects;

import javax.measure.Unit;

/**
 * A positive data capacity. Established KB/MB/GB/TB archive text retains its
 * historical binary scaling and display, while the units themselves use IEC
 * binary prefixes.
 */
public final class DataCapacity extends PositiveQuantity<DataCapacity> {

	private static final long serialVersionUID = 1L;

	public DataCapacity(final Number amount, final Unit<DataCapacity> unit) {
		super(amount, unit);
	}

	public DataCapacity multiply(final int factor) {
		if (factor <= 0) {
			throw new IllegalArgumentException("Capacity factor must be positive: " + factor);
		}
		return new DataCapacity(amount().multiply(BigDecimal.valueOf(factor)), unit());
	}

	public boolean sameSizeAs(final DataCapacity other) {
		return isEquivalentTo(Objects.requireNonNull(other, "other"));
	}

	public BigDecimal inKibibytes() {
		return amountIn(MeasurementUnits.KIBIBYTE);
	}

	/**
	 * Returns the value in the historically labelled binary KB archive unit.
	 */
	public BigDecimal inKilobytes() {
		return inKibibytes();
	}

	@Override
	public String toString() {
		return amount().toPlainString() + symbol(unit());
	}

	private static String symbol(final Unit<DataCapacity> unit) {
		if (MeasurementUnits.BYTE.equals(unit)) {
			return "B";
		}
		if (MeasurementUnits.KIBIBYTE.equals(unit)) {
			return "KB";
		}
		if (MeasurementUnits.MEBIBYTE.equals(unit)) {
			return "MB";
		}
		if (MeasurementUnits.GIBIBYTE.equals(unit)) {
			return "GB";
		}
		if (MeasurementUnits.TEBIBYTE.equals(unit)) {
			return "TB";
		}
		return " " + unit;
	}
}
