package com.retrocrawler.mycollection.catalog;

public enum Tested {

	POST("post"),
	BOOT("boot"),
	FULL("full");

	private final String value;

	Tested(final String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}

	@Override
	public String toString() {
		return value;
	}
}
