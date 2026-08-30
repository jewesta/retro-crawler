package com.retrocrawler.mcp.filter;

import java.util.List;
import java.util.Objects;

/** The stable key, human name, and query behavior of one model Fact filter. */
public record FilterSummary(String key, String name, FilterKind kind, boolean multiple, List<FilterOperator> operators,
		List<String> choices) {

	public FilterSummary {
		key = Objects.requireNonNull(key, "key").strip();
		name = Objects.requireNonNull(name, "name").strip();
		if (key.isEmpty()) {
			throw new IllegalArgumentException("A filter key must not be blank.");
		}
		if (name.isEmpty()) {
			throw new IllegalArgumentException("A filter name must not be blank.");
		}
		Objects.requireNonNull(kind, "kind");
		Objects.requireNonNull(operators, "operators");
		operators = List.copyOf(operators);
		if (operators.isEmpty()) {
			throw new IllegalArgumentException("A filter must expose at least one operator.");
		}
		Objects.requireNonNull(choices, "choices");
		choices = List.copyOf(choices);
		if (kind != FilterKind.CHOICES && !choices.isEmpty()) {
			throw new IllegalArgumentException("Only a CHOICES filter may expose choices.");
		}
	}
}
