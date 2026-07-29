package com.retrocrawler.mycollection.catalog;

public enum Destiny {

	VERSCHENKT("verschenkt"),
	VERKAUFT("verkauft"),
	ENTSORGT("entsorgt"),
	GESCHLACHTET("geschlachtet"),
	RETOURNIERT("retourniert"),
	GESTOHLEN("gestohlen");

	private final String value;

	Destiny(final String value) {
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
