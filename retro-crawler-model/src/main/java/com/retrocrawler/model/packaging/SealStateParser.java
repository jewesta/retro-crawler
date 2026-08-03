package com.retrocrawler.model.packaging;

import java.util.Locale;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;

/**
 * Parses canonical English package-seal observations.
 */
public final class SealStateParser implements FactParser {

	@Override
	public RatedFact parse(final String rawValue) {
		if (rawValue == null) {
			return noMatch();
		}

		return switch (rawValue.trim().toLowerCase(Locale.ROOT)) {
		case "sealed" -> RatedFact.exact(SealState.SEALED);
		case "opened" -> RatedFact.exact(SealState.OPENED);
		default -> noMatch();
		};
	}

	private static RatedFact noMatch() {
		return RatedFact.none("Expected a recognized seal state.");
	}
}
