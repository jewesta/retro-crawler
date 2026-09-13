package com.retrocrawler.model.measurement;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

import javax.measure.Quantity;
import javax.measure.Unit;

import tech.units.indriya.quantity.Quantities;

/**
 * Base for immutable archive measurements whose observed magnitude must be
 * positive.
 *
 * @param <Q>
 *            the JSR 385 kind of quantity
 */
public abstract class PositiveQuantity<Q extends Quantity<Q>> implements Quantity<Q>, Serializable {

	private static final long serialVersionUID = 1L;

	private final BigDecimal value;
	private final Unit<Q> unit;

	protected PositiveQuantity(final Number value, final Unit<Q> unit) {
		this.unit = Objects.requireNonNull(unit, "unit");
		this.value = decimal(Objects.requireNonNull(value, "value")).stripTrailingZeros();
		if (this.value.signum() <= 0) {
			throw new IllegalArgumentException("Quantity must be positive: " + value);
		}
	}

	@Override
	public final BigDecimal getValue() {
		return value;
	}

	@Override
	public final Unit<Q> getUnit() {
		return unit;
	}

	@Override
	public final Scale getScale() {
		return Scale.ABSOLUTE;
	}

	/** Returns the magnitude in the unit in which this value was observed. */
	public final BigDecimal amount() {
		return value;
	}

	/** Returns the unit in which this value was observed. */
	public final Unit<Q> unit() {
		return getUnit();
	}

	/** Converts this value and returns its magnitude in the requested unit. */
	public final BigDecimal amountIn(final Unit<Q> targetUnit) {
		return decimal(to(Objects.requireNonNull(targetUnit, "targetUnit")).getValue()).stripTrailingZeros();
	}

	@Override
	public final Quantity<Q> add(final Quantity<Q> addend) {
		return delegate().add(addend);
	}

	@Override
	public final Quantity<Q> subtract(final Quantity<Q> subtrahend) {
		return delegate().subtract(subtrahend);
	}

	@Override
	public final Quantity<?> divide(final Quantity<?> divisor) {
		return delegate().divide(divisor);
	}

	@Override
	public final Quantity<Q> divide(final Number divisor) {
		return delegate().divide(divisor);
	}

	@Override
	public final Quantity<?> multiply(final Quantity<?> multiplicand) {
		return delegate().multiply(multiplicand);
	}

	@Override
	public final Quantity<Q> multiply(final Number multiplicand) {
		return delegate().multiply(multiplicand);
	}

	@Override
	public final Quantity<?> inverse() {
		return delegate().inverse();
	}

	@Override
	public final Quantity<Q> negate() {
		return delegate().negate();
	}

	@Override
	public final Quantity<Q> to(final Unit<Q> targetUnit) {
		return delegate().to(targetUnit);
	}

	@Override
	public final <T extends Quantity<T>> Quantity<T> asType(final Class<T> type) throws ClassCastException {
		return delegate().asType(type);
	}

	@Override
	public final boolean isEquivalentTo(final Quantity<Q> other) {
		return delegate().isEquivalentTo(other);
	}

	@Override
	public final boolean equals(final Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		final PositiveQuantity<?> quantity = (PositiveQuantity<?>) other;
		return value.equals(quantity.getValue()) && unit.equals(quantity.getUnit())
				&& getScale() == quantity.getScale();
	}

	@Override
	public final int hashCode() {
		return Objects.hash(value, unit, getScale());
	}

	@Override
	public String toString() {
		return delegate().toString();
	}

	private Quantity<Q> delegate() {
		return Quantities.getQuantity(value, getUnit(), getScale());
	}

	private static BigDecimal decimal(final Number value) {
		if (value instanceof final BigDecimal decimal) {
			return decimal;
		}
		return new BigDecimal(value.toString());
	}
}
