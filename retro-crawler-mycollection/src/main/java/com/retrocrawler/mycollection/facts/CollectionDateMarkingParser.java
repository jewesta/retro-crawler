package com.retrocrawler.mycollection.facts;

import java.util.Locale;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.model.temporal.DateMarkingParser;

/** Maps the collection's German calendar-week notation to canonical ISO. */
public final class CollectionDateMarkingParser implements FactParser {

	private final DateMarkingParser delegate = new DateMarkingParser();

	@Override
	public RatedFact parse(final String rawValue) {
		return delegate.parse(rawValue == null ? null : canonical(rawValue));
	}

	private static String canonical(final String rawValue) {
		return rawValue.trim().toUpperCase(Locale.ROOT).replaceFirst("^(\\d{4})-KW(\\d{2})$", "$1-W$2");
	}
}
