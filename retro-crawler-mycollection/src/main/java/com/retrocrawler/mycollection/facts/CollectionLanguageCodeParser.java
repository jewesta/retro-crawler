package com.retrocrawler.mycollection.facts;

import java.util.Locale;
import java.util.Set;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.model.locale.LanguageCodeParser;

/**
 * Restricts language recognition to spellings established in this collection.
 * The shared parser remains the portable canonical parser; this adapter admits
 * only collection meanings and deliberately never interprets {@code EU} as a
 * language.
 */
public final class CollectionLanguageCodeParser implements FactParser {

	private static final Set<String> OBSERVED_LANGUAGE_MARKERS = Set.of("DE", "EN", "ES", "FR", "GERMAN", "IT");

	private final LanguageCodeParser delegate = new LanguageCodeParser();

	@Override
	public RatedFact parse(final String rawValue) {
		if (rawValue == null || !OBSERVED_LANGUAGE_MARKERS.contains(rawValue.trim().toUpperCase(Locale.ROOT))) {
			return RatedFact.none("Expected an established collection language marker.");
		}
		return delegate.parse(rawValue);
	}
}
