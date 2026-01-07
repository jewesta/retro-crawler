package com.retrocrawler.app.collection.model;

import com.retrocrawler.core.gear.RatedFact;

public class ID {

	public static int MIN_ID = 200000;

	private final int id;

	public ID(final int id) {
		this.id = id;
	}

	public static RatedFact valueOf(final String value) {
		if (value.length() != 6) {
			return RatedFact.none("Expected 6 digits but was " + value.length() + ".");
		}
		try {
			final int id = Integer.parseInt(value);
			if (id >= MIN_ID) {
				return RatedFact.exact(new ID(id));
			}
			return RatedFact.none("The number must be bigger or equal than " + MIN_ID + " but was " + id + ".");
		} catch (final NumberFormatException e) {
			// Not numerical
			return RatedFact.none("Expected an integer but was " + value + ".");
		}
	}

	@Override
	public String toString() {
		return "" + id;
	}

}
