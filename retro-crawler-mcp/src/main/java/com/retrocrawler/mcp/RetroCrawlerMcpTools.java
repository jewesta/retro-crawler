package com.retrocrawler.mcp;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;

import com.retrocrawler.core.RetroCrawler;
import com.retrocrawler.mcp.filter.FilterCatalog;
import com.retrocrawler.mcp.filter.FilterRegistry;
import com.retrocrawler.mcp.search.FilterCriterion;
import com.retrocrawler.mcp.search.GearSearch;
import com.retrocrawler.mcp.search.SearchGearResult;

/** MCP tools backed by RetroCrawler's public, transport-neutral API. */
public final class RetroCrawlerMcpTools {

	private final RetroCrawler retroCrawler;

	private final FilterRegistry filters;

	private final GearSearch gearSearch;

	public RetroCrawlerMcpTools(final RetroCrawler retroCrawler) {
		this.retroCrawler = Objects.requireNonNull(retroCrawler, "retroCrawler");
		this.filters = new FilterRegistry(retroCrawler.collectionId(), retroCrawler.filters());
		this.gearSearch = new GearSearch(retroCrawler, filters);
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

	@McpTool(name = "search_gear", title = "Search RetroCrawler Gear",
			description = "Search the current immutable Stash by logical archive and strict model filters. "
					+ "Every criterion must match. Results contain stable ARIs rather than filesystem paths.",
			generateOutputSchema = true, annotations = @McpTool.McpAnnotations(readOnlyHint = true,
					destructiveHint = false, idempotentHint = true, openWorldHint = false))
	public SearchGearResult searchGear(@McpToolParam(required = false,
			description = "Logical archive IDs from list_archives. Omit or use an empty list for every archive.") final List<String> archiveIds,
			@McpToolParam(required = false,
					description = "Strict filter criteria using IDs, operators, and values from list_filters. All criteria are combined with AND.") final List<FilterCriterion> criteria,
			@McpToolParam(required = false,
					description = "Zero-based result offset. Defaults to 0.") final Integer offset,
			@McpToolParam(required = false,
					description = "Maximum results to return. Defaults to 25 and cannot exceed 100.") final Integer limit)
			throws IOException {
		return gearSearch.search(archiveIds, criteria, offset, limit);
	}
}
