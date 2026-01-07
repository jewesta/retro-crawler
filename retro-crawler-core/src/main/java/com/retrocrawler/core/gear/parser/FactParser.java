package com.retrocrawler.core.gear.parser;

import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.Confidence;
import com.retrocrawler.core.gear.RatedFact;

public interface FactParser {

	/**
	 * The parser always get handed a single value via rawValue. If the
	 * {@link Clue}'s value is a Collection of Strings, then parse will be called
	 * once for every String.
	 * 
	 * {@link Clue#isAnonymous()} can be used to boost / lower the
	 * {@link Confidence}. If the clue is keyed (not anonymous) then it is usually
	 * much more plausible that the value meets our expectations.
	 */
	RatedFact parse(String rawValue);

}
