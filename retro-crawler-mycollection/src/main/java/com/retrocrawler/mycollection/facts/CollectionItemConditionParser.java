package com.retrocrawler.mycollection.facts;

import java.util.Locale;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.model.condition.ItemConditionParser;

/**
 * Maps the collection's German condition markers to the portable item
 * condition vocabulary.
 */
public final class CollectionItemConditionParser implements FactParser {

	private final ItemConditionParser delegate = new ItemConditionParser();

	@Override
	public RatedFact parse(final String rawValue) {
		return delegate.parse(rawValue == null ? null : canonical(rawValue));
	}

	private static String canonical(final String rawValue) {
		return switch (rawValue.trim().toLowerCase(Locale.ROOT)) {
		case "neu" -> "new";
		case "gebraucht" -> "used";
		case "beschädigt" -> "damaged";
		default -> rawValue;
		};
	}
}
