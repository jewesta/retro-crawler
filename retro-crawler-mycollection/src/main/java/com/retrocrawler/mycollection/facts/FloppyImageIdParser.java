package com.retrocrawler.mycollection.facts;

import java.util.Locale;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.mycollection.model.FloppyImageId;

public final class FloppyImageIdParser implements FactParser {

	@Override
	public RatedFact parse(final String rawValue) {
		if (rawValue == null || !rawValue.toUpperCase(Locale.ROOT).matches("FD-\\d+")) {
			return RatedFact.none("Expected an FD-<number> floppy image ID.");
		}
		return RatedFact.exact(new FloppyImageId(rawValue));
	}
}
