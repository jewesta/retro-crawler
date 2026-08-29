package com.retrocrawler.mycollection.facts;

import java.util.Locale;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.EnumFactParser;
import com.retrocrawler.core.gear.parser.ParseContext;
import com.retrocrawler.model.packaging.SealState;
import com.retrocrawler.model.packaging.SealStateParser;

/**
 * Maps the collection's German seal-state markers to the portable packaging
 * vocabulary.
 */
public final class CollectionSealStateParser implements EnumFactParser<SealState> {

	@Override
	public Class<SealState> enumType() {
		return SealState.class;
	}

	private final SealStateParser delegate = new SealStateParser();

	@Override
	public RatedFact<SealState> parse(final String rawValue, final ParseContext context) {
		return delegate.parse(rawValue == null ? null : canonical(rawValue), context);
	}

	private static String canonical(final String rawValue) {
		return switch (rawValue.trim().toLowerCase(Locale.ROOT)) {
		case "versiegelt" -> "sealed";
		case "geöffnet" -> "opened";
		default -> rawValue;
		};
	}
}
