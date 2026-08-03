package com.retrocrawler.model.condition;

import java.util.Locale;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;

/**
 * Parses canonical English functional-condition observations.
 */
public final class FunctionalConditionParser implements FactParser {

	@Override
	public RatedFact parse(final String rawValue) {
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

	private static RatedFact noMatch() {
		return RatedFact.none("Expected a recognized functional condition.");
	}
}
