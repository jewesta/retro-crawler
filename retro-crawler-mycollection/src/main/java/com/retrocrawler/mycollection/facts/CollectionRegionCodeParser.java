package com.retrocrawler.mycollection.facts;

import java.util.Locale;
import java.util.Set;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.core.gear.parser.ParseContext;
import com.retrocrawler.model.locale.RegionCode;
import com.retrocrawler.model.locale.RegionCodeParser;

/**
 * Restricts anonymous region recognition to spellings established in this
 * collection so unrelated two-letter technical abbreviations remain available
 * to their own parsers. The collection's historical {@code EU} spelling is
 * normalized to Nintendo's {@code EUR} European release-region code.
 */
public final class CollectionRegionCodeParser implements FactParser<RegionCode> {

	private static final Set<String> OBSERVED_REGION_MARKERS = Set.of("DE", "ES", "EU", "EUR", "FR", "IT", "JP", "US");

	private final RegionCodeParser delegate = new RegionCodeParser();

	@Override
	public RatedFact<RegionCode> parse(final String rawValue, final ParseContext context) {
		if (rawValue == null) {
			return RatedFact.none("Expected an established collection region marker.");
		}

		final String marker = rawValue.trim().toUpperCase(Locale.ROOT);
		if (!OBSERVED_REGION_MARKERS.contains(marker)) {
			return RatedFact.none("Expected an established collection region marker.");
		}
		return delegate.parse("EU".equals(marker) ? "EUR" : marker, context);
	}
}
