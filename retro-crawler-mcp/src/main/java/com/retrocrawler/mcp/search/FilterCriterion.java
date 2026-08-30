package com.retrocrawler.mcp.search;

import java.util.Objects;

import com.retrocrawler.mcp.filter.FilterOperator;

/** One strict Fact-filter criterion in an MCP Gear search. */
public record FilterCriterion(String filterKey, FilterOperator operator, String value) {

	public static final int MAXIMUM_VALUE_LENGTH = 1_000;

	public FilterCriterion {
		if (Objects.requireNonNull(filterKey, "filterKey").isBlank()) {
			throw new IllegalArgumentException("filterKey must not be blank.");
		}
		Objects.requireNonNull(operator, "operator");
		if (Objects.requireNonNull(value, "value").isEmpty()) {
			throw new IllegalArgumentException("value must not be empty.");
		}
		if (value.length() > MAXIMUM_VALUE_LENGTH) {
			throw new IllegalArgumentException("value must not exceed " + MAXIMUM_VALUE_LENGTH + " characters.");
		}
	}
}
