package com.retrocrawler.model.hardware;

import java.util.Locale;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.core.gear.parser.ParseContext;

public final class MemoryFeatureParser implements FactParser<MemoryFeature> {

	@Override
	public RatedFact<MemoryFeature> parse(final String rawValue, final ParseContext context) {
		if (rawValue == null) {
			return RatedFact.none("Expected a recognized memory feature.");
		}

		return switch (rawValue.trim().toUpperCase(Locale.ROOT)) {
		case "ECC" -> RatedFact.exact(MemoryFeature.ECC);
		case "EDO" -> RatedFact.exact(MemoryFeature.EXTENDED_DATA_OUT);
		case "FPM" -> RatedFact.exact(MemoryFeature.FAST_PAGE_MODE);
		case "REG", "REGISTERED" -> RatedFact.exact(MemoryFeature.REGISTERED);
		default -> RatedFact.none("Expected a recognized memory feature.");
		};
	}
}
