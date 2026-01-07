package com.retrocrawler.core.gear.parser;

import com.retrocrawler.core.gear.RatedFact;

public class IntParser implements FactParser {

	@Override
	public RatedFact parse(final String rawValue) {
		Integer integer;
		try {
			integer = Integer.valueOf(rawValue);
		} catch (final NumberFormatException e) {
			return RatedFact.none("Expected numerical string but got " + rawValue);
		}
		// Confidence for a number without any context is always weak
		return RatedFact.weak(integer);
	}

}
