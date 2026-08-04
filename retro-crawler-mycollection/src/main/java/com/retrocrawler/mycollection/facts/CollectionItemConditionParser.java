package com.retrocrawler.mycollection.facts;

import java.util.Locale;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParseContext;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.model.condition.ItemCondition;
import com.retrocrawler.model.condition.ItemConditionParser;

/**
 * Maps the collection's German condition markers to the portable item condition
 * vocabulary.
 */
public final class CollectionItemConditionParser implements FactParser<ItemCondition> {

	private final ItemConditionParser delegate = new ItemConditionParser();

	@Override
	public RatedFact<ItemCondition> parse(final String rawValue, final FactParseContext context) {
		return delegate.parse(rawValue == null ? null : canonical(rawValue), context);
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
