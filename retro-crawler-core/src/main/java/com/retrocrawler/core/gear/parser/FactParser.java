package com.retrocrawler.core.gear.parser;

import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.Confidence;
import com.retrocrawler.core.gear.RatedFact;

public interface FactParser<T> {

	/**
	 * The parser is handed one raw value. If the {@link Clue}'s value contains
	 * multiple strings, this method is called once for every string and returns
	 * at most one typed interpretation of each observation.
	 * 
	 * {@link Clue#isAnonymous()} can be used to boost / lower the
	 * {@link Confidence}. If the clue is keyed (not anonymous) then it is
	 * usually much more plausible that the value meets our expectations.
	 */
	RatedFact<T> parse(String rawValue, FactParseContext context);

}
