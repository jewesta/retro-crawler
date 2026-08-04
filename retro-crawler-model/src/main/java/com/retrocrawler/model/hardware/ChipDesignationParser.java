package com.retrocrawler.model.hardware;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParseContext;
import com.retrocrawler.core.gear.parser.FactParser;

/**
 * Parses a nonblank, explicitly identified chip designation without assuming a
 * manufacturer-specific part-number syntax.
 * <p>
 * This permissive parser is intended for keyed clues. Consumers should not use
 * it to claim arbitrary anonymous text.
 */
public final class ChipDesignationParser implements FactParser<ChipDesignation> {

	@Override
	public RatedFact<ChipDesignation> parse(final String rawValue, final FactParseContext context) {
		if (rawValue == null || rawValue.isBlank()) {
			return RatedFact.none("Expected a nonblank chip designation.");
		}
		return RatedFact.exact(new ChipDesignation(rawValue));
	}
}
