package com.retrocrawler.model.identifier;

import java.util.Locale;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;

public final class SegaGameGearCartridgeCodeParser implements FactParser {

	@Override
	public RatedFact parse(final String rawValue) {
		if (rawValue == null) {
			return noMatch();
		}
		final String normalized = rawValue.trim().toUpperCase(Locale.ROOT)
				.replaceFirst("^GG[\\s-]*", "");
		if (!normalized.matches("\\d{4}")) {
			return noMatch();
		}
		return RatedFact.exact(new SegaGameGearCartridgeCode(normalized));
	}

	private static RatedFact noMatch() {
		return RatedFact.none("Expected a four-digit Sega Game Gear cartridge code, optionally prefixed by GG.");
	}
}
