package com.retrocrawler.core.progress;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Dependency-free calculations shared by progressors. */
public final class ProgressUtils {

	private static final BigDecimal ONE = BigDecimal.ONE;

	private ProgressUtils() {
		// Static utility.
	}

	/** Converts relative weights to whole percentages which add up to 100. */
	public static int[] toPercentagesBase100(final double... values) {
		final BigDecimal[] decimals = toPercentages(2, values);
		final int[] percentages = new int[decimals.length];
		for (int index = 0; index < decimals.length; index++) {
			percentages[index] = decimals[index].movePointRight(2).intValueExact();
		}
		return percentages;
	}

	/**
	 * Converts relative weights to fixed-scale shares which add up exactly to
	 * one.
	 */
	public static BigDecimal[] toPercentages(final int decimals, final double... values) {
		if (decimals < 0) {
			throw new IllegalArgumentException("Decimal places must not be negative.");
		}
		if (values.length == 0) {
			throw new IllegalArgumentException("Require at least one progress weight.");
		}

		double total = 0;
		for (final double value : values) {
			if (!Double.isFinite(value) || value < 0) {
				throw new IllegalArgumentException("Progress weights must be finite and non-negative.");
			}
			total += value;
		}
		if (!Double.isFinite(total) || total <= 0) {
			throw new IllegalArgumentException("The sum of progress weights must be finite and positive.");
		}

		final BigDecimal increment = BigDecimal.ONE.movePointLeft(decimals);
		final BigDecimal[] percentages = new BigDecimal[values.length];
		final List<Remainder> remainders = new ArrayList<>(values.length);
		BigDecimal sum = BigDecimal.ZERO;
		for (int index = 0; index < values.length; index++) {
			final BigDecimal precise = BigDecimal.valueOf(values[index] / total);
			final BigDecimal rounded = precise.setScale(decimals, RoundingMode.DOWN);
			percentages[index] = rounded;
			sum = sum.add(rounded);
			remainders.add(new Remainder(index, precise.subtract(rounded)));
		}

		remainders.sort(Comparator.comparing(Remainder::remainder).reversed());
		int remainderIndex = 0;
		for (BigDecimal missing = ONE.subtract(sum); missing.signum() > 0; missing = missing.subtract(increment)) {
			final int index = remainders.get(remainderIndex++ % remainders.size()).index();
			percentages[index] = percentages[index].add(increment);
		}
		return percentages;
	}

	private record Remainder(int index, BigDecimal remainder) {
	}
}
