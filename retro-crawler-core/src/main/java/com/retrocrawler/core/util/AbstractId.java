package com.retrocrawler.core.util;

import com.fasterxml.jackson.annotation.JsonValue;

public class AbstractId<T> implements Id<T> {

	private final T id;

	public AbstractId(final T version) {
		this.id = version;
	}

	@Override
	@JsonValue
	public T get() {
		return id;
	}

	@Override
	public String toString() {
		return String.valueOf(id);
	}

}