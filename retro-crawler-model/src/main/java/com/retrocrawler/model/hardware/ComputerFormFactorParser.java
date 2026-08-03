package com.retrocrawler.model.hardware;

import java.util.Locale;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;

public final class ComputerFormFactorParser implements FactParser<ComputerFormFactor> {

	@Override
	public RatedFact<ComputerFormFactor> parse(final String rawValue) {
		if (rawValue == null) {
			return RatedFact.none("Expected a recognized computer form factor.");
		}

		final String normalized = rawValue.trim().toUpperCase(Locale.ROOT).replace('Μ', 'M').replace("-", "")
				.replace(" ", "");
		return switch (normalized) {
		case "AT" -> RatedFact.exact(ComputerFormFactor.AT);
		case "ATX" -> RatedFact.exact(ComputerFormFactor.ATX);
		case "LPX" -> RatedFact.exact(ComputerFormFactor.LPX);
		case "MATX", "MICROATX" -> RatedFact.exact(ComputerFormFactor.MICRO_ATX);
		case "MINIITX" -> RatedFact.exact(ComputerFormFactor.MINI_ITX);
		case "NLX" -> RatedFact.exact(ComputerFormFactor.NLX);
		default -> RatedFact.none("Expected a recognized computer form factor.");
		};
	}
}
