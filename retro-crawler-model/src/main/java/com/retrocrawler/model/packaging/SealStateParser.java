package com.retrocrawler.model.packaging;

import java.util.Locale;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.EnumFactParser;
import com.retrocrawler.core.gear.parser.ParseContext;

/**
 * Parses canonical English package-seal observations.
 */
public final class SealStateParser implements EnumFactParser<SealState> {

	@Override
	public Class<SealState> enumType() {
		return SealState.class;
	}

	@Override
	public RatedFact<SealState> parse(final String rawValue, final ParseContext context) {
		if (rawValue == null) {
			return noMatch();
		}

		return switch (rawValue.trim().toLowerCase(Locale.ROOT)) {
		case "sealed" -> RatedFact.exact(SealState.SEALED);
		case "opened" -> RatedFact.exact(SealState.OPENED);
		default -> noMatch();
		};
	}

	private static RatedFact<SealState> noMatch() {
		return RatedFact.none("Expected a recognized seal state.");
	}
}
