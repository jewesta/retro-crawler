package com.retrocrawler.mcp.filter;

import java.util.List;
import java.util.Objects;

/** The stable identity and query behavior of one collection-model filter. */
public record FilterSummary(String id, FilterKind kind, boolean multiple, List<FilterOperator> operators,
		List<String> choices) {

	public FilterSummary {
		Objects.requireNonNull(id, "id");
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
