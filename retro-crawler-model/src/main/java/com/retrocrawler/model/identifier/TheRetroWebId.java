package com.retrocrawler.model.identifier;

/**
 * A positive numeric database ID used by The Retro Web.
 *
 * <p>
 * The ID does not identify its content category. Combine it with a
 * {@link TheRetroWebCategory} to obtain an unambiguous
 * {@link TheRetroWebReference}.
 */
public record TheRetroWebId(int value) {

	public TheRetroWebId {
		if (value <= 0) {
			throw new IllegalArgumentException("The Retro Web ID must be positive: " + value);
		}
	}

	@Override
	public String toString() {
		return Integer.toString(value);
	}
}
