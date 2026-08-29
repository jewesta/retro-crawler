package com.retrocrawler.model.packaging;

import java.util.Locale;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.EnumFactParser;
import com.retrocrawler.core.gear.parser.ParseContext;

/**
 * Parses canonical English packaging-origin observations.
 */
public final class PackagingOriginParser implements EnumFactParser<PackagingOrigin> {

	@Override
	public Class<PackagingOrigin> enumType() {
		return PackagingOrigin.class;
	}

	@Override
	public RatedFact<PackagingOrigin> parse(final String rawValue, final ParseContext context) {
		if (rawValue == null) {
			return noMatch();
		}

		return switch (rawValue.trim().toLowerCase(Locale.ROOT)) {
		case "original packaging" -> RatedFact.exact(PackagingOrigin.ORIGINAL);
		default -> noMatch();
		};
	}

	private static RatedFact<PackagingOrigin> noMatch() {
		return RatedFact.none("Expected a recognized packaging origin.");
	}
}
