package com.retrocrawler.mcp.filter;

import java.util.Objects;

/** The stable identity and query behavior of one collection-model filter. */
public record FilterSummary(String id, FilterKind kind, boolean multiple) {

	public FilterSummary {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(kind, "kind");
	}
}
