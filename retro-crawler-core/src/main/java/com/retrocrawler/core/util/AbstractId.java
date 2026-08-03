package com.retrocrawler.core.util;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonValue;

public class AbstractId<T> implements Id<T> {

	private final T id;

	public AbstractId(final T id) {
		this.id = Objects.requireNonNull(id, "id");
	}

	@Override
	@JsonValue
	public T value() {
		return id;
	}

	@Override
	public String toString() {
		return String.valueOf(id);
	}

	@Override
	public final int hashCode() {
		return Objects.hash(getClass(), id);
	}

	@Override
	public final boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final AbstractId<?> other = (AbstractId<?>) obj;
		return id.equals(other.id);
	}

}
