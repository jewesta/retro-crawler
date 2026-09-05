package com.retrocrawler.model.software;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.core.gear.parser.ParseContext;

public final class VersionParser implements FactParser<Version> {

	@Override
	public RatedFact<Version> parse(final String rawValue, final ParseContext context) {
		if (rawValue == null) {
			return noMatch();
		}
		try {
			/*
			 * The deliberately broad v-plus-digit syntax is characteristic but
			 * not unambiguous. Manufacturer markings and other strict
			 * identifiers can begin the same way and should win anonymous-clue
			 * classification at EXACT.
			 */
			return RatedFact.strong(new Version(rawValue));
		} catch (final IllegalArgumentException e) {
			return noMatch();
		}
	}

	private static RatedFact<Version> noMatch() {
		return RatedFact.none("Expected v or V followed by a digit and optional version text.");
	}
}
