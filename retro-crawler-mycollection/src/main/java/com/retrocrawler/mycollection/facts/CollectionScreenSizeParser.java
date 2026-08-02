package com.retrocrawler.mycollection.facts;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.model.measurement.ScreenSize;
import com.retrocrawler.model.measurement.ScreenSizeParser;

/** Restricts anonymous screen-size recognition to sizes established here. */
public final class CollectionScreenSizeParser implements FactParser {

	private static final ScreenSize OBSERVED_SCREEN_SIZE = new ScreenSize(java.math.BigDecimal.valueOf(19));

	private final ScreenSizeParser delegate = new ScreenSizeParser();

	@Override
	public RatedFact parse(final String rawValue) {
		final RatedFact parsed = delegate.parse(rawValue);
		if (parsed.getValue().filter(OBSERVED_SCREEN_SIZE::equals).isPresent()) {
			return parsed;
		}
		return RatedFact.none("Expected the collection's established 19-inch screen size.");
	}
}
