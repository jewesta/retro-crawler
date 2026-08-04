package com.retrocrawler.mycollection.facts;

import java.util.Locale;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParseContext;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.model.temporal.DateMarking;
import com.retrocrawler.model.temporal.DateMarkingParser;

/** Maps the collection's German calendar-week notation to canonical ISO. */
public final class CollectionDateMarkingParser implements FactParser<DateMarking> {

	private final DateMarkingParser delegate = new DateMarkingParser();

	@Override
	public RatedFact<DateMarking> parse(final String rawValue, final FactParseContext context) {
		return delegate.parse(rawValue == null ? null : canonical(rawValue), context);
	}

	private static String canonical(final String rawValue) {
		return rawValue.trim().toUpperCase(Locale.ROOT).replaceFirst("^(\\d{4})-KW(\\d{2})$", "$1-W$2");
	}
}
