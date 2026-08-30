package com.retrocrawler.mcp;

import java.util.List;
import java.util.Objects;

/** The registered archives exposed by a RetroCrawler MCP server. */
public record ArchiveCatalog(String collectionId, List<ArchiveSummary> archives) {

	public ArchiveCatalog {
		Objects.requireNonNull(collectionId, "collectionId");
		archives = List.copyOf(archives);
	}
}
