package com.retrocrawler.model.identifier;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParseContext;
import com.retrocrawler.core.gear.parser.FactParser;

public final class MacAddressParser implements FactParser<MacAddress> {

	@Override
	public RatedFact<MacAddress> parse(final String rawValue, final FactParseContext context) {
		try {
			return RatedFact.exact(new MacAddress(rawValue));
		} catch (final IllegalArgumentException | NullPointerException e) {
			return RatedFact.none("Expected a 48-bit MAC address.");
		}
	}
}
