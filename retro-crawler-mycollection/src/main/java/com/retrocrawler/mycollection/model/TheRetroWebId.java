package com.retrocrawler.mycollection.model;

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
