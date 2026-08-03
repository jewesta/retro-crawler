package com.retrocrawler.demo.catalog;

public record DemoId(int value) {

	private static final int FIRST_ID = 200_000;

	private static final int LAST_ID = 299_999;

	public DemoId {
		if (value < FIRST_ID || value > LAST_ID) {
			throw new IllegalArgumentException("Demo ID must be in the 2-series namespace: " + value);
		}
	}

	@Override
	public String toString() {
		return Integer.toString(value);
	}
}
