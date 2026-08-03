package com.retrocrawler.model.packaging;

import java.util.Locale;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;

/**
 * Parses canonical English packaging-origin observations.
 */
public final class PackagingOriginParser implements FactParser {

	@Override
	public RatedFact parse(final String rawValue) {
		if (rawValue == null) {
			return noMatch();
		}

		return switch (rawValue.trim().toLowerCase(Locale.ROOT)) {
		case "original packaging" -> RatedFact.exact(PackagingOrigin.ORIGINAL);
		default -> noMatch();
		};
	}

	private static RatedFact noMatch() {
		return RatedFact.none("Expected a recognized packaging origin.");
	}
}
