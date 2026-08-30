package com.retrocrawler.core.gear.parser;

import java.util.Comparator;

import com.retrocrawler.core.gear.RatedFact;

public class IntParser implements OrderedFactParser<Integer> {

	@Override
	public Comparator<? super Integer> order() {
		return Comparator.naturalOrder();
	}

	@Override
	public RatedFact<Integer> parse(final String rawValue, final ParseContext context) {
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
