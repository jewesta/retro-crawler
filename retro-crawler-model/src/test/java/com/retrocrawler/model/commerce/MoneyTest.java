package com.retrocrawler.model.commerce;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.Currency;

import org.junit.jupiter.api.Test;

class MoneyTest {

	private static final Currency EUR = Currency.getInstance("EUR");

	@Test
	void representsEquivalentAmountsCanonically() {
		assertEquals(new Money(new BigDecimal("100"), EUR),
				new Money(new BigDecimal("100.00"), EUR));
		assertEquals("100 EUR", new Money(new BigDecimal("100.00"), EUR).toString());
	}

	@Test
	void remainsAValueRatherThanPricePolicy() {
		assertEquals("-5 EUR", new Money(new BigDecimal("-5"), EUR).toString());
	}
}
