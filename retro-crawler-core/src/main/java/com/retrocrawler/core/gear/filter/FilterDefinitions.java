package com.retrocrawler.core.gear.filter;

import java.util.List;
import java.util.Objects;

/** A view exposing the structured filters defined by one immutable model. */
public interface FilterDefinitions {

	/**
	 * Every structured filter inherited from the immutable collection model.
	 */
	List<FilterDefinition<?>> filters();

	/**
	 * Selects all model filters or only filters populated in this data set.
	 * Relevant filters retain their model-defined order.
	 */
	default List<FilterDefinition<?>> filters(final FilterSelection selection) {
		return switch (Objects.requireNonNull(selection, "selection")) {
		case ALL -> filters();
		case RELEVANT -> filters().stream().filter(filter -> availability(filter).populatedOccurrences() > 0).toList();
		};
	}

	/** Lazily computes this data set's availability for one model filter. */
	<T> FilterAvailability<T> availability(FilterDefinition<T> filter);
}
