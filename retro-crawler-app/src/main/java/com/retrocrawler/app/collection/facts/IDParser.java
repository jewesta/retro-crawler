package com.retrocrawler.app.collection.facts;

import com.retrocrawler.app.collection.model.ID;
import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;

public class IDParser implements FactParser {

	@Override
	public RatedFact parse(final String rawValue) {
		return ID.valueOf(rawValue);
	}

}
