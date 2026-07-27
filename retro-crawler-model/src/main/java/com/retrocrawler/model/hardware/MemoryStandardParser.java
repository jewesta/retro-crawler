package com.retrocrawler.model.hardware;

import java.util.Locale;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;

public final class MemoryStandardParser implements FactParser {

	@Override
	public RatedFact parse(final String rawValue) {
		if (rawValue == null) {
			return RatedFact.none("Expected a recognized memory standard.");
		}

		return switch (rawValue.trim().toUpperCase(Locale.ROOT).replace("-", "")) {
		case "PC66" -> RatedFact.exact(MemoryStandard.PC_66);
		case "PC100" -> RatedFact.exact(MemoryStandard.PC_100);
		case "PC133" -> RatedFact.exact(MemoryStandard.PC_133);
		default -> RatedFact.none("Expected a recognized memory standard.");
		};
	}
}
