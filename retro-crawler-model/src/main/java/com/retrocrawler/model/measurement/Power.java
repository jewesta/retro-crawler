package com.retrocrawler.model.measurement;

import java.math.BigDecimal;
import java.util.Objects;

public record Power(BigDecimal watts) {

	public Power {
		Objects.requireNonNull(watts, "watts");
		if (watts.signum() <= 0) {
			throw new IllegalArgumentException("Power must be positive: " + watts);
		}
		watts = watts.stripTrailingZeros();
	}

	@Override
	public String toString() {
		return watts.toPlainString() + "W";
	}
}
