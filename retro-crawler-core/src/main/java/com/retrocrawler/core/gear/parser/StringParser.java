package com.retrocrawler.core.gear.parser;

import com.retrocrawler.core.gear.RatedFact;

public class StringParser implements FactParser<String> {

	@Override
	public RatedFact<String> parse(final String rawValue, final ParseContext context) {
		// Confidence for a number without any context is always weak
		return RatedFact.weak(rawValue);
	}

}
