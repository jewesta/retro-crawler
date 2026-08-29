package com.retrocrawler.core.gear.parser;

import java.util.Comparator;

import com.retrocrawler.core.gear.filter.FilterType;

/** A parser producing values with meaningful lower and upper bounds. */
public interface OrderedFactParser<T> extends FactParser<T> {

	Comparator<? super T> order();

	@Override
	default FilterType<T> filterType() {
		return FilterType.range(order());
	}
}
