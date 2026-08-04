package com.retrocrawler.model.identifier;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;

import de.creativecouple.validation.isbn.ISBN;

public final class ISBNParser implements FactParser<ISBN> {

	@Override
	public RatedFact<ISBN> parse(final String rawValue) {
		try {
			return RatedFact.exact(ISBN.valueOf(rawValue));
		} catch (final NumberFormatException e) {
			return RatedFact.none("Expected an ISBN number but got " + rawValue);
		}
	}
}
