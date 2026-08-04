package com.retrocrawler.mycollection.facts;

import java.util.Locale;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.core.gear.parser.ParseContext;
import com.retrocrawler.mycollection.catalog.FloppyImageId;

public final class FloppyImageIdParser implements FactParser<FloppyImageId> {

	@Override
	public RatedFact<FloppyImageId> parse(final String rawValue, final ParseContext context) {
		if (rawValue == null || !rawValue.toUpperCase(Locale.ROOT).matches("FD-\\d+")) {
			return RatedFact.none("Expected an FD-<number> floppy image ID.");
		}
		return RatedFact.exact(new FloppyImageId(rawValue));
	}
}
