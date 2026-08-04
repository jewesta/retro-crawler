package com.retrocrawler.mycollection.facts;

import java.util.Locale;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.model.packaging.PackagingOrigin;
import com.retrocrawler.model.packaging.PackagingOriginParser;

/**
 * Maps the collection's original-packaging marker to the portable packaging
 * vocabulary.
 */
public final class CollectionPackagingOriginParser implements FactParser<PackagingOrigin> {

	private final PackagingOriginParser delegate = new PackagingOriginParser();

	@Override
	public RatedFact<PackagingOrigin> parse(final String rawValue) {
		return delegate.parse(rawValue == null ? null : canonical(rawValue));
	}

	private static String canonical(final String rawValue) {
		return switch (rawValue.trim().toLowerCase(Locale.ROOT)) {
		case "ovp" -> "original packaging";
		default -> rawValue;
		};
	}
}
