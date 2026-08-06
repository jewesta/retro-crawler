package com.retrocrawler.mycollection.facts;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.core.gear.parser.ParseContext;
import com.retrocrawler.mycollection.catalog.MyRetroId;

public final class MyRetroIdParser implements FactParser<MyRetroId> {

	@Override
	public RatedFact<MyRetroId> parse(final String rawValue, final ParseContext context) {
		if (rawValue == null || !rawValue.matches("2\\d{5}")) {
			return RatedFact.none("Expected a 2-series six-digit Retro ID.");
		}
		return RatedFact.exact(new MyRetroId(Integer.parseInt(rawValue)));
	}
}
