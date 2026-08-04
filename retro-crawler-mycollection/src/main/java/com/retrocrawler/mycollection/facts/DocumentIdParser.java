package com.retrocrawler.mycollection.facts;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.core.gear.parser.ParseContext;
import com.retrocrawler.mycollection.catalog.DocumentId;

public final class DocumentIdParser implements FactParser<DocumentId> {

	@Override
	public RatedFact<DocumentId> parse(final String rawValue, final ParseContext context) {
		if (rawValue == null || !rawValue.matches("1\\d{5}")) {
			return RatedFact.none("Expected a 1-series six-digit document ID.");
		}
		return RatedFact.exact(new DocumentId(Integer.parseInt(rawValue)));
	}
}
