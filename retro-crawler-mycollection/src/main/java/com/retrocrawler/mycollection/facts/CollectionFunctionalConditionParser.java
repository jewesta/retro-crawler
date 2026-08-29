package com.retrocrawler.mycollection.facts;

import java.util.Locale;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.EnumFactParser;
import com.retrocrawler.core.gear.parser.ParseContext;
import com.retrocrawler.model.condition.FunctionalCondition;
import com.retrocrawler.model.condition.FunctionalConditionParser;

/**
 * Maps the collection's German health markers to the portable functional
 * condition vocabulary.
 */
public final class CollectionFunctionalConditionParser implements EnumFactParser<FunctionalCondition> {

	@Override
	public Class<FunctionalCondition> enumType() {
		return FunctionalCondition.class;
	}

	private final FunctionalConditionParser delegate = new FunctionalConditionParser();

	@Override
	public RatedFact<FunctionalCondition> parse(final String rawValue, final ParseContext context) {
		return delegate.parse(rawValue == null ? null : canonical(rawValue), context);
	}

	private static String canonical(final String rawValue) {
		return switch (rawValue.trim().toLowerCase(Locale.ROOT)) {
		case "defekt" -> "defective";
		case "teildefekt" -> "partially defective";
		default -> rawValue;
		};
	}
}
