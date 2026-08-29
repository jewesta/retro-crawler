package com.retrocrawler.mycollection.facts;

import java.util.Locale;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.EnumFactParser;
import com.retrocrawler.core.gear.parser.ParseContext;
import com.retrocrawler.model.packaging.PackagingOrigin;
import com.retrocrawler.model.packaging.PackagingOriginParser;

/**
 * Maps the collection's original-packaging marker to the portable packaging
 * vocabulary.
 */
public final class CollectionPackagingOriginParser implements EnumFactParser<PackagingOrigin> {

	@Override
	public Class<PackagingOrigin> enumType() {
		return PackagingOrigin.class;
	}

	private final PackagingOriginParser delegate = new PackagingOriginParser();

	@Override
	public RatedFact<PackagingOrigin> parse(final String rawValue, final ParseContext context) {
		return delegate.parse(rawValue == null ? null : canonical(rawValue), context);
	}

	private static String canonical(final String rawValue) {
		return switch (rawValue.trim().toLowerCase(Locale.ROOT)) {
		case "ovp" -> "original packaging";
		default -> rawValue;
		};
	}
}
