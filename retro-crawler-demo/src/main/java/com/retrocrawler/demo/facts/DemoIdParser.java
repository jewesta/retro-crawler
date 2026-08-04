package com.retrocrawler.demo.facts;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.demo.catalog.DemoId;

public final class DemoIdParser implements FactParser<DemoId> {

	@Override
	public RatedFact<DemoId> parse(final String rawValue) {
		if (rawValue == null || !rawValue.matches("2\\d{5}")) {
			return RatedFact.none("Expected a 2-series six-digit demo ID.");
		}
		return RatedFact.exact(new DemoId(Integer.parseInt(rawValue)));
	}
}
