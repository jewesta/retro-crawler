package com.retrocrawler.core.gear.parser;

import java.util.List;

import com.retrocrawler.core.gear.filter.FilterType;

/** A parser producing values from one closed, ordered domain. */
public interface ChoiceFactParser<T> extends FactParser<T> {

	List<T> choices();

	@Override
	default FilterType<T> filterType() {
		return FilterType.choices(choices());
	}
}
