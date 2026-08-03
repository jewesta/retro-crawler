package com.retrocrawler.mycollection.facts;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.mycollection.gear.GearKind;

/** Parses the collection's deliberately short Gear-type markers. */
public final class GearKindParser implements FactParser<GearKind> {

	@Override
	public RatedFact<GearKind> parse(final String rawValue) {
		if (rawValue != null && "HDD".equalsIgnoreCase(rawValue.trim())) {
			return RatedFact.exact(GearKind.HARD_DISK_DRIVE);
		}
		return RatedFact.none("Expected a recognized collection Gear-type marker.");
	}
}
