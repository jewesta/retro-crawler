package com.retrocrawler.mycollection.facts;

import java.util.Locale;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.model.condition.DamageKind;
import com.retrocrawler.model.condition.DamageKindParser;

/**
 * Maps specific German damage markers from the collection to portable damage
 * kinds without also claiming a second, broader condition fact.
 */
public final class CollectionDamageKindParser implements FactParser<DamageKind> {

	private final DamageKindParser delegate = new DamageKindParser();

	@Override
	public RatedFact<DamageKind> parse(final String rawValue) {
		return delegate.parse(rawValue == null ? null : canonical(rawValue));
	}

	private static String canonical(final String rawValue) {
		return switch (rawValue.trim().toLowerCase(Locale.ROOT)) {
		case "akkuschaden" -> "battery damage";
		case "bruch" -> "breakage";
		default -> rawValue;
		};
	}
}
