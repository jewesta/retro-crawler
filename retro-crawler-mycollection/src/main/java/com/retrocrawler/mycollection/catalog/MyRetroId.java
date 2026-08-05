package com.retrocrawler.mycollection.catalog;

public record MyRetroId(int value) {

	private static final int FIRST_ID = 200_000;

	private static final int LAST_ID = 299_999;

	public MyRetroId {
		if (value < FIRST_ID || value > LAST_ID) {
			throw new IllegalArgumentException("Retro ID must be in the 2-series namespace: " + value);
		}
	}

	@Override
	public String toString() {
		return Integer.toString(value);
	}
}
