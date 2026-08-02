package com.retrocrawler.core.gear.parser;

import java.util.Collection;

import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.Confidence;
import com.retrocrawler.core.gear.RatedFact;

public interface FactParser {

	/**
	 * The parser always get handed a single value via rawValue. If the
	 * {@link Clue}'s value is a Collection of Strings, then parse will be called
	 * once for every String.
	 * <p>
	 * A parser may return a {@link Collection} when one raw observation states
	 * multiple values. The fact finder flattens those values for a collection
	 * target and rejects them for a scalar target.
	 * 
	 * {@link Clue#isAnonymous()} can be used to boost / lower the
	 * {@link Confidence}. If the clue is keyed (not anonymous) then it is usually
	 * much more plausible that the value meets our expectations.
	 */
	RatedFact parse(String rawValue);

	/**
	 * Parses one raw value with its runtime archive location. Parsers whose
	 * interpretation is location-independent inherit the traditional behavior.
	 */
	default RatedFact parse(final String rawValue, final FactParseContext context) {
		return parse(rawValue);
	}

}
