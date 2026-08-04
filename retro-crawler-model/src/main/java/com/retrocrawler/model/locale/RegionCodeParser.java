package com.retrocrawler.model.locale;

import java.util.Locale;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.core.gear.parser.ParseContext;

public final class RegionCodeParser implements FactParser<RegionCode> {

	@Override
	public RatedFact<RegionCode> parse(final String rawValue, final ParseContext context) {
		if (rawValue == null) {
			return noMatch();
		}

		final String code = rawValue.trim().toUpperCase(Locale.ROOT);
		if (!RegionCode.isSupported(code)) {
			return noMatch();
		}
		return RatedFact.exact(new RegionCode(code));
	}

	private static RatedFact<RegionCode> noMatch() {
		return RatedFact
				.none("Expected an ISO 3166-1 alpha-2 region code or a supported industry release-market code.");
	}
}
