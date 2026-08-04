package com.retrocrawler.model.measurement;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;

public final class ScreenSizeParser implements FactParser<ScreenSize> {

	private static final Pattern SCREEN_SIZE = Pattern.compile("^(\\d+(?:[,.]\\d+)?)\\s*(?:\"|″|in(?:ch(?:es)?)?)$",
			Pattern.CASE_INSENSITIVE);

	@Override
	public RatedFact<ScreenSize> parse(final String rawValue) {
		if (rawValue == null) {
			return noMatch();
		}
		final Matcher matcher = SCREEN_SIZE.matcher(rawValue.trim());
		if (!matcher.matches()) {
			return noMatch();
		}
		try {
			final BigDecimal inches = new BigDecimal(matcher.group(1).replace(',', '.'));
			/*
			 * A unit-qualified length is strong evidence, but an anonymous clue
			 * does not prove that the length describes a screen. More specific
			 * form-factor parsers can therefore win with exact confidence.
			 */
			return RatedFact.strong(new ScreenSize(inches));
		} catch (final IllegalArgumentException e) {
			return noMatch();
		}
	}

	private static RatedFact<ScreenSize> noMatch() {
		return RatedFact.none("Expected a positive screen diagonal with an inch unit.");
	}
}
