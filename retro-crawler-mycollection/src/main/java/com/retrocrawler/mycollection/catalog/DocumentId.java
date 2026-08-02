package com.retrocrawler.mycollection.catalog;

/** Identifies a scanned or downloaded document in the collection catalogue. */
public record DocumentId(int value) {

	private static final int FIRST_ID = 100_000;

	private static final int LAST_ID = 199_999;

	public DocumentId {
		if (value < FIRST_ID || value > LAST_ID) {
			throw new IllegalArgumentException("Document ID must be in the 1-series namespace: " + value);
		}
	}

	@Override
	public String toString() {
		return Integer.toString(value);
	}
}
