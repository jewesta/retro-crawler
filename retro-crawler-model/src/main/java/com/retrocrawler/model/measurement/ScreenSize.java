package com.retrocrawler.model.measurement;

import java.math.BigDecimal;
import java.util.Objects;

/** The conventionally quoted diagonal size of a display, measured in inches. */
public record ScreenSize(BigDecimal diagonalInches) {

	public ScreenSize {
		Objects.requireNonNull(diagonalInches, "diagonalInches");
		if (diagonalInches.signum() <= 0) {
			throw new IllegalArgumentException("Screen size must be positive: " + diagonalInches);
		}
		diagonalInches = diagonalInches.stripTrailingZeros();
	}

	@Override
	public String toString() {
		return diagonalInches.toPlainString() + '"';
	}
}
