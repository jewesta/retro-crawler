package com.retrocrawler.core.gear.filter;

import java.util.List;

/** A view exposing the structured filters defined by one immutable model. */
public interface FilterDefinitions {

	/**
	 * Every structured filter inherited from the immutable collection model.
	 */
	List<FilterDefinition<?>> filters();
	
}
