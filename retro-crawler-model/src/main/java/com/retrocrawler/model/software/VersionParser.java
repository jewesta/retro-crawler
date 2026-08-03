package com.retrocrawler.model.software;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;

public final class VersionParser implements FactParser<Version> {

	@Override
	public RatedFact<Version> parse(final String rawValue) {
		if (rawValue == null) {
			return noMatch();
		}
		try {
			return RatedFact.exact(new Version(rawValue));
		} catch (final IllegalArgumentException e) {
			return noMatch();
		}
	}

	private static RatedFact<Version> noMatch() {
		return RatedFact.none("Expected v or V followed by a digit and optional version text.");
	}
}
