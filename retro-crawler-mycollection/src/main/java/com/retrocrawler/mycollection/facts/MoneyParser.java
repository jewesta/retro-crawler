package com.retrocrawler.mycollection.facts;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.model.commerce.Money;

/**
 * Parses the collection's monetary convention, including its default currency.
 */
public final class MoneyParser implements FactParser<Money> {

	private static final Currency DEFAULT_CURRENCY = Currency.getInstance("EUR");

	private static final Pattern MONEY = Pattern.compile("^(\\d+(?:[.,]\\d{1,2})?)\\s*([A-Za-z]{3})?$");

	@Override
	public RatedFact<Money> parse(final String rawValue) {
		if (rawValue == null) {
			return RatedFact.none("Expected a non-null monetary amount.");
		}

		final Matcher matcher = MONEY.matcher(rawValue.trim());
		if (!matcher.matches()) {
			return RatedFact.none("Expected an amount optionally followed by a three-letter currency code.");
		}

		final BigDecimal amount = new BigDecimal(matcher.group(1).replace(',', '.'));
		final String rawCurrency = matcher.group(2);
		final Currency currency;
		try {
			currency = rawCurrency == null ? DEFAULT_CURRENCY
					: Currency.getInstance(rawCurrency.toUpperCase(Locale.ROOT));
		} catch (final IllegalArgumentException e) {
			return RatedFact.none("Unknown currency code: " + rawCurrency);
		}
		return RatedFact.exact(new Money(amount, currency));
	}
}
