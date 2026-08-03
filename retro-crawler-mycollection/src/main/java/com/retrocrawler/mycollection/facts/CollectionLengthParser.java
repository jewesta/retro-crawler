package com.retrocrawler.mycollection.facts;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.model.measurement.Length;
import com.retrocrawler.model.measurement.LengthParser;

/** Adapts the collection's legacy inch glyph to the portable length parser. */
public final class CollectionLengthParser implements FactParser<Length> {

	private final LengthParser delegate = new LengthParser();

	@Override
	public RatedFact<Length> parse(final String rawValue) {
		return delegate.parse(rawValue == null ? null : rawValue.replace('\uF020', '"'));
	}
}
