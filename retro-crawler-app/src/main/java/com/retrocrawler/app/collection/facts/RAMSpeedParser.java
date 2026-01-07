package com.retrocrawler.app.collection.facts;

import com.retrocrawler.app.collection.model.RAMSpeed;
import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.AttributeValueParseException;
import com.retrocrawler.core.gear.parser.FactParser;

public class RAMSpeedParser implements FactParser {

	@Override
	public RatedFact parse(final String rawValue) {
		switch (rawValue) {
		case "ns80", "80ns":
			return RatedFact.exact(RAMSpeed.ns80);
		case "ns70", "70ns":
			return RatedFact.exact(RAMSpeed.ns70);
		case "ns60", "60ns":
			return RatedFact.exact(RAMSpeed.ns60);
		case "pc100":
			return RatedFact.exact(RAMSpeed.pc100);
		case "pc133":
			return RatedFact.exact(RAMSpeed.pc133);
		default:
			throw new AttributeValueParseException();
		}
	}

}
