package com.retrocrawler.mycollection.facts;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.mycollection.catalog.RetroId;

public final class RetroIdParser implements FactParser<RetroId> {

	@Override
	public RatedFact<RetroId> parse(final String rawValue) {
		if (rawValue == null || !rawValue.matches("2\\d{5}")) {
			return RatedFact.none("Expected a 2-series six-digit Retro ID.");
		}
		return RatedFact.exact(new RetroId(Integer.parseInt(rawValue)));
	}
}
