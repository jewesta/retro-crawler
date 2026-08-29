package com.retrocrawler.core.gear.parser;

import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.gear.Confidence;
import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.filter.FilterType;

public interface FactParser<T> {

	/**
	 * Describes how values produced by this parser participate in structured
	 * filtering. Parsers with richer semantics override the exact default,
	 * usually by implementing a specialized parser interface.
	 */
	default FilterType<T> filterType() {
		return FilterType.exact();
	}

	/**
	 * The parser is handed one raw value. If the {@link Clue}'s value contains
	 * multiple strings, this method is called once for every string and returns
	 * at most one typed interpretation of each observation.
	 * 
	 * {@link Clue#isAnonymous()} can be used to boost / lower the
	 * {@link Confidence}. If the clue is keyed (not anonymous) then it is
	 * usually much more plausible that the value meets our expectations.
	 */
	RatedFact<T> parse(String rawValue, ParseContext context);

}
