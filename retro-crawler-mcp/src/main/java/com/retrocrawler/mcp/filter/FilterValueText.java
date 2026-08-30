package com.retrocrawler.mcp.filter;

import java.util.Objects;

/** The protocol text used to identify and display one typed filter value. */
public final class FilterValueText {

	private FilterValueText() {
	}

	public static String from(final Object value) {
		return Objects.requireNonNull(Objects.requireNonNull(value, "value").toString(), "value.toString()");
	}
}
