package com.retrocrawler.mycollection.catalog;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

public record Price(BigDecimal amount, Currency currency) {

	public Price {
		Objects.requireNonNull(amount, "amount");
		Objects.requireNonNull(currency, "currency");
		if (amount.signum() < 0) {
			throw new IllegalArgumentException("Price must not be negative.");
		}
	}

	@Override
	public String toString() {
		return amount.toPlainString() + " " + currency.getCurrencyCode();
	}
}
