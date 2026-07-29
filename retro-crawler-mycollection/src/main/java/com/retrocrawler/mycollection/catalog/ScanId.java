package com.retrocrawler.mycollection.catalog;

public record ScanId(int value) {

	private static final int FIRST_ID = 100_000;

	private static final int LAST_ID = 199_999;

	public ScanId {
		if (value < FIRST_ID || value > LAST_ID) {
			throw new IllegalArgumentException("Scan ID must be in the 1-series namespace: " + value);
		}
	}

	@Override
	public String toString() {
		return Integer.toString(value);
	}
}
