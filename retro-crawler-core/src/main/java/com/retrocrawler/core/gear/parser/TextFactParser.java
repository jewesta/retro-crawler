package com.retrocrawler.core.gear.parser;

import com.retrocrawler.core.gear.filter.FilterType;

/** A parser producing values with textual matching semantics. */
public interface TextFactParser extends FactParser<String> {

	@Override
	default FilterType<String> filterType() {
		return FilterType.text();
	}
}
