package com.retrocrawler.demo.collection.facts;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;

import de.creativecouple.validation.isbn.ISBN;

public class ISBNParser implements FactParser {

	@Override
	public RatedFact parse(final String rawValue) {
		try {
			return RatedFact.exact(ISBN.valueOf(rawValue));
		} catch (final NumberFormatException e) {
			return RatedFact.none("Expected an ISBN number but got " + rawValue);
		}
	}

}
