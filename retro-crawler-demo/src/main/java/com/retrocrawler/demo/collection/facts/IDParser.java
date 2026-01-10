package com.retrocrawler.demo.collection.facts;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.demo.collection.model.ID;

public class IDParser implements FactParser {

	@Override
	public RatedFact parse(final String rawValue) {
		return ID.valueOf(rawValue);
	}

}
