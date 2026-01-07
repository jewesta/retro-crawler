package com.retrocrawler.core.gear.parser;

import com.retrocrawler.core.gear.RatedFact;

public class StringParser implements FactParser {

	@Override
	public RatedFact parse(final String rawValue) {
		// Confidence for a number without any context is always weak
		return RatedFact.weak(rawValue);
	}

}
