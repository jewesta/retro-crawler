package com.retrocrawler.model.identifier;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;

public final class PlayStationPortableDiscIdParser implements FactParser {

	private static final Pattern DISC_ID = Pattern.compile("^([A-Z]{4})[- ]?(\\d{5})$");

	@Override
	public RatedFact parse(final String rawValue) {
		if (rawValue == null) {
			return noMatch();
		}

		final Matcher matcher = DISC_ID.matcher(rawValue.trim().toUpperCase(Locale.ROOT));
		if (!matcher.matches()) {
			return noMatch();
		}

		return PlayStationPortableDiscPrefix.fromCode(matcher.group(1))
				.<RatedFact>map(prefix -> RatedFact.exact(new PlayStationPortableDiscId(prefix, matcher.group(2))))
				.orElseGet(PlayStationPortableDiscIdParser::noMatch);
	}

	private static RatedFact noMatch() {
		return RatedFact.none("Expected a known physical PlayStation Portable disc ID.");
	}
}
