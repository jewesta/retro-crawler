package com.retrocrawler.model.commerce;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

/**
 * A monetary amount in an ISO 4217 currency.
 */
public record Money(BigDecimal amount, Currency currency) {

	public Money {
		amount = Objects.requireNonNull(amount, "amount").stripTrailingZeros();
		Objects.requireNonNull(currency, "currency");
	}

	@Override
	public String toString() {
		return amount.toPlainString() + " " + currency.getCurrencyCode();
	}
}
