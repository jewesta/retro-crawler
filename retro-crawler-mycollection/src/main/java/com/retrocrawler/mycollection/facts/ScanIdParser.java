package com.retrocrawler.mycollection.facts;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.mycollection.catalog.ScanId;

public final class ScanIdParser implements FactParser {

	@Override
	public RatedFact parse(final String rawValue) {
		if (rawValue == null || !rawValue.matches("1\\d{5}")) {
			return RatedFact.none("Expected a 1-series six-digit scan ID.");
		}
		return RatedFact.exact(new ScanId(Integer.parseInt(rawValue)));
	}
}
