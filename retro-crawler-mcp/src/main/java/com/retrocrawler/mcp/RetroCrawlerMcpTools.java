package com.retrocrawler.mcp;

import java.util.Objects;

import org.springframework.ai.mcp.annotation.McpTool;

import com.retrocrawler.core.RetroCrawler;
import com.retrocrawler.mcp.filter.FilterCatalog;
import com.retrocrawler.mcp.filter.FilterRegistry;

/** MCP tools backed by RetroCrawler's public, transport-neutral API. */
public final class RetroCrawlerMcpTools {

	private final RetroCrawler retroCrawler;

	private final FilterRegistry filters;

	public RetroCrawlerMcpTools(final RetroCrawler retroCrawler) {
		this.retroCrawler = Objects.requireNonNull(retroCrawler, "retroCrawler");
		this.filters = new FilterRegistry(retroCrawler.collectionId(), retroCrawler.filters());
	}

	@McpTool(name = "list_archives", title = "List RetroCrawler archives",
			description = "List the logical identities of all archives registered with RetroCrawler.",
			generateOutputSchema = true, annotations = @McpTool.McpAnnotations(readOnlyHint = true,
					destructiveHint = false, idempotentHint = true, openWorldHint = false))
	public ArchiveCatalog listArchives() {
		return new ArchiveCatalog(retroCrawler.collectionId(), retroCrawler.archives().stream()
				.map(archive -> new ArchiveSummary(archive.id().value(), archive.name())).toList());
	}

	@McpTool(name = "list_filters", title = "List RetroCrawler filters",
			description = "List the stable filter identities and filtering behavior defined by the collection model.",
			generateOutputSchema = true, annotations = @McpTool.McpAnnotations(readOnlyHint = true,
					destructiveHint = false, idempotentHint = true, openWorldHint = false))
	public FilterCatalog listFilters() {
		return filters.catalog();
	}
}
