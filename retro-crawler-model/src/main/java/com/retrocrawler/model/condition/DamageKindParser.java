package com.retrocrawler.model.condition;

import java.util.Locale;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.EnumFactParser;
import com.retrocrawler.core.gear.parser.ParseContext;

/**
 * Parses canonical English names for specific damage kinds.
 */
public final class DamageKindParser implements EnumFactParser<DamageKind> {

	@Override
	public Class<DamageKind> enumType() {
		return DamageKind.class;
	}

	@Override
	public RatedFact<DamageKind> parse(final String rawValue, final ParseContext context) {
		if (rawValue == null) {
			return noMatch();
		}

		return switch (rawValue.trim().toLowerCase(Locale.ROOT)) {
		case "battery damage" -> RatedFact.exact(DamageKind.BATTERY_DAMAGE);
		case "breakage" -> RatedFact.exact(DamageKind.BREAKAGE);
		default -> noMatch();
		};
	}

	private static RatedFact<DamageKind> noMatch() {
		return RatedFact.none("Expected a recognized kind of damage.");
	}
}
