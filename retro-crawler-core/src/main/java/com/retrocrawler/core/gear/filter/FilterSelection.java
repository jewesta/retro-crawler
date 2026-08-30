package com.retrocrawler.core.gear.filter;

/** Selects model-wide or data-set-relevant filter definitions. */
public enum FilterSelection {

	/** Every filter defined by the model. */
	ALL,

	/** Filters with at least one populated Gear occurrence in the data set. */
	RELEVANT
}
