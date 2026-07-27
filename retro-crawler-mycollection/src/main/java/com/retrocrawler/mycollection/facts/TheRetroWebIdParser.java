package com.retrocrawler.mycollection.facts;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.mycollection.model.TheRetroWebId;

public final class TheRetroWebIdParser implements FactParser {

	@Override
	public RatedFact parse(final String rawValue) {
		if (rawValue == null || !rawValue.matches("\\d+")) {
			return RatedFact.none("Expected a positive numeric The Retro Web database ID.");
		}

		try {
			return RatedFact.exact(new TheRetroWebId(Integer.parseInt(rawValue)));
		} catch (final IllegalArgumentException e) {
			return RatedFact.none("Expected a positive numeric The Retro Web database ID.");
		}
	}
}
