package com.retrocrawler.model.measurement;

import java.math.BigDecimal;
import java.util.Objects;

public record DataCapacity(BigDecimal amount, Unit unit) {

	public DataCapacity {
		Objects.requireNonNull(amount, "amount");
		Objects.requireNonNull(unit, "unit");
		if (amount.signum() <= 0) {
			throw new IllegalArgumentException("Data capacity must be positive: " + amount);
		}
		amount = amount.stripTrailingZeros();
	}

	public DataCapacity multiply(final int factor) {
		if (factor <= 0) {
			throw new IllegalArgumentException("Capacity factor must be positive: " + factor);
		}
		return new DataCapacity(amount.multiply(BigDecimal.valueOf(factor)), unit);
	}

	public boolean sameSizeAs(final DataCapacity other) {
		Objects.requireNonNull(other, "other");
		return inKilobytes().compareTo(other.inKilobytes()) == 0;
	}

	public BigDecimal inKilobytes() {
		return amount.multiply(unit.kilobytes());
	}

	@Override
	public String toString() {
		return amount.toPlainString() + unit;
	}

	public enum Unit {

		KB(BigDecimal.ONE),
		MB(BigDecimal.valueOf(1_024)),
		GB(BigDecimal.valueOf(1_024L * 1_024)),
		TB(BigDecimal.valueOf(1_024L * 1_024 * 1_024));

		private final BigDecimal kilobytes;

		Unit(final BigDecimal kilobytes) {
			this.kilobytes = kilobytes;
		}

		BigDecimal kilobytes() {
			return kilobytes;
		}
	}
}
