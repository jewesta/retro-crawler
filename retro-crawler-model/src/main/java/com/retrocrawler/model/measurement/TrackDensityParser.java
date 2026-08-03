package com.retrocrawler.model.measurement;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;

public final class TrackDensityParser implements FactParser<TrackDensity> {

	private static final Pattern TRACKS_PER_INCH = Pattern.compile("^(\\d+)\\s*TPI$", Pattern.CASE_INSENSITIVE);

	@Override
	public RatedFact<TrackDensity> parse(final String rawValue) {
		if (rawValue == null) {
			return RatedFact.none("Expected a positive track density in TPI.");
		}

		final Matcher matcher = TRACKS_PER_INCH.matcher(rawValue.trim());
		if (!matcher.matches()) {
			return RatedFact.none("Expected a positive track density in TPI.");
		}

		try {
			return RatedFact.exact(new TrackDensity(Integer.parseInt(matcher.group(1))));
		} catch (final IllegalArgumentException e) {
			return RatedFact.none("Expected a positive track density in TPI.");
		}
	}
}
