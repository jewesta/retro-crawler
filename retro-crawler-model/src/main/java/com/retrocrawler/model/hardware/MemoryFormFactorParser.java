package com.retrocrawler.model.hardware;

import java.util.Locale;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParseContext;
import com.retrocrawler.core.gear.parser.FactParser;

public final class MemoryFormFactorParser implements FactParser<MemoryFormFactor> {

	@Override
	public RatedFact<MemoryFormFactor> parse(final String rawValue, final FactParseContext context) {
		if (rawValue == null) {
			return RatedFact.none("Expected a recognized memory form factor.");
		}

		final String normalized = rawValue.trim().toUpperCase(Locale.ROOT).replace("-", "").replace(" ", "");
		return switch (normalized) {
		case "DIP" -> RatedFact.exact(MemoryFormFactor.DIP);
		case "SIPP" -> RatedFact.exact(MemoryFormFactor.SIPP);
		case "SIMM30", "30PINSIMM" -> RatedFact.exact(MemoryFormFactor.SIMM_30_PIN);
		case "SIMM72", "72PINSIMM" -> RatedFact.exact(MemoryFormFactor.SIMM_72_PIN);
		case "DIMM" -> RatedFact.exact(MemoryFormFactor.DIMM);
		case "SODIMM" -> RatedFact.exact(MemoryFormFactor.SO_DIMM);
		case "RIMM" -> RatedFact.exact(MemoryFormFactor.RIMM);
		default -> RatedFact.none("Expected a recognized memory form factor.");
		};
	}
}
