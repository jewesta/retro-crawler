package com.retrocrawler.mcp.filter;

import java.util.List;
import java.util.Objects;

/** Every filter exposed by a RetroCrawler MCP server. */
public record FilterCatalog(String collectionId, List<FilterSummary> filters) {

	public FilterCatalog {
		Objects.requireNonNull(collectionId, "collectionId");
		Objects.requireNonNull(filters, "filters");
		filters = List.copyOf(filters);
	}
}
