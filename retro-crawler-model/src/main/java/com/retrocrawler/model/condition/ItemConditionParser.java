package com.retrocrawler.model.condition;

import java.util.Locale;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.EnumFactParser;
import com.retrocrawler.core.gear.parser.ParseContext;

/**
 * Parses the canonical English names of broad item conditions.
 */
public final class ItemConditionParser implements EnumFactParser<ItemCondition> {

	@Override
	public Class<ItemCondition> enumType() {
		return ItemCondition.class;
	}

	@Override
	public RatedFact<ItemCondition> parse(final String rawValue, final ParseContext context) {
		if (rawValue == null) {
			return noMatch();
		}

		return switch (rawValue.trim().toLowerCase(Locale.ROOT)) {
		case "new" -> RatedFact.exact(ItemCondition.NEW);
		case "used" -> RatedFact.exact(ItemCondition.USED);
		case "refurbished" -> RatedFact.exact(ItemCondition.REFURBISHED);
		case "damaged" -> RatedFact.exact(ItemCondition.DAMAGED);
		default -> noMatch();
		};
	}

	private static RatedFact<ItemCondition> noMatch() {
		return RatedFact.none("Expected a recognized item condition.");
	}
}
