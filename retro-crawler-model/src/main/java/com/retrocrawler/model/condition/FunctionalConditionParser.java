package com.retrocrawler.model.condition;

import java.util.Locale;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.core.gear.parser.ParseContext;

/**
 * Parses canonical English functional-condition observations.
 */
public final class FunctionalConditionParser implements FactParser<FunctionalCondition> {

	@Override
	public RatedFact<FunctionalCondition> parse(final String rawValue, final ParseContext context) {
		if (rawValue == null) {
			return noMatch();
		}

		return switch (rawValue.trim().toLowerCase(Locale.ROOT)) {
		case "working" -> RatedFact.exact(FunctionalCondition.WORKING);
		case "partially defective" -> RatedFact.exact(FunctionalCondition.PARTIALLY_DEFECTIVE);
		case "defective" -> RatedFact.exact(FunctionalCondition.DEFECTIVE);
		default -> noMatch();
		};
	}

	private static RatedFact<FunctionalCondition> noMatch() {
		return RatedFact.none("Expected a recognized functional condition.");
	}
}
