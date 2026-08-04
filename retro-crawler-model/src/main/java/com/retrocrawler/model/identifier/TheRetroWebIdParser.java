package com.retrocrawler.model.identifier;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParseContext;
import com.retrocrawler.core.gear.parser.FactParser;

public final class TheRetroWebIdParser implements FactParser<TheRetroWebId> {

	@Override
	public RatedFact<TheRetroWebId> parse(final String rawValue, final FactParseContext context) {
		if (rawValue == null || !rawValue.trim().matches("\\d+")) {
			return RatedFact.none("Expected a positive numeric The Retro Web database ID.");
		}

		try {
			return RatedFact.exact(new TheRetroWebId(Integer.parseInt(rawValue.trim())));
		} catch (final IllegalArgumentException e) {
			return RatedFact.none("Expected a positive numeric The Retro Web database ID.");
		}
	}
}
