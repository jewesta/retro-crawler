package com.retrocrawler.model.condition;

import java.util.Locale;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;

/**
 * Parses canonical English names for specific damage kinds.
 */
public final class DamageKindParser implements FactParser {

	@Override
	public RatedFact parse(final String rawValue) {
		if (rawValue == null) {
			return noMatch();
		}

		return switch (rawValue.trim().toLowerCase(Locale.ROOT)) {
		case "battery damage" -> RatedFact.exact(DamageKind.BATTERY_DAMAGE);
		case "breakage" -> RatedFact.exact(DamageKind.BREAKAGE);
		default -> noMatch();
		};
	}

	private static RatedFact noMatch() {
		return RatedFact.none("Expected a recognized kind of damage.");
	}
}
