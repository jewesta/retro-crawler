package com.retrocrawler.core.gear.filter;

import java.util.List;

import com.retrocrawler.core.util.ModelNames;

/**
 * Immutable query metadata derived from every {@code @RetroFact} binding that
 * shares one semantic Fact key.
 */
public interface FilterDefinition<T> {

	/** The semantic Fact key represented by this filter. */
	String key();

	/** The human-facing name of the represented Fact. */
	default String name() {
		return ModelNames.displayName(key());
	}

	/** The common value type produced for this Fact key. */
	Class<T> valueType();

	/** Whether one Gear may carry more than one value for this Fact. */
	boolean multiple();

	/**
	 * The structured operations and declared domain supported by the parser.
	 */
	FilterType<T> filterType();

	/** Every model Gear type to which this Fact applies. */
	List<Class<?>> gearTypes();

	/** Whether this Fact is declared for the supplied Gear occurrence. */
	boolean appliesTo(Object gear);

	/**
	 * Reads the resolved Fact values from the supplied Gear. An empty list
	 * means that the Fact applies but has no resolved value, or does not apply.
	 */
	List<T> values(Object gear);
}
