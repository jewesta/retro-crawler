package com.retrocrawler.model.identifier;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;

public final class ISBNParser implements FactParser {

	@Override
	public RatedFact parse(final String rawValue) {
		try {
			return RatedFact.exact(new ISBN(rawValue));
		} catch (final IllegalArgumentException | NullPointerException e) {
			return RatedFact.none("Expected a valid ISBN-10 or ISBN-13.");
		}
	}
}
