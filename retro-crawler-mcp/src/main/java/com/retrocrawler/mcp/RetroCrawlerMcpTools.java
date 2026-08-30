package com.retrocrawler.mcp;

import java.util.Objects;

import org.springframework.ai.mcp.annotation.McpTool;

import com.retrocrawler.core.RetroCrawler;

/** MCP tools backed by RetroCrawler's public, transport-neutral API. */
public final class RetroCrawlerMcpTools {

	private final RetroCrawler retroCrawler;

	public RetroCrawlerMcpTools(final RetroCrawler retroCrawler) {
		this.retroCrawler = Objects.requireNonNull(retroCrawler, "retroCrawler");
	}

	@McpTool(name = "list_archives", title = "List RetroCrawler archives",
			description = "List the logical identities of all archives registered with RetroCrawler.",
			generateOutputSchema = true, annotations = @McpTool.McpAnnotations(readOnlyHint = true,
					destructiveHint = false, idempotentHint = true, openWorldHint = false))
	public ArchiveCatalog listArchives() {
		return new ArchiveCatalog(retroCrawler.collectionId(), retroCrawler.archives().stream()
				.map(archive -> new ArchiveSummary(archive.id().value(), archive.name())).toList());
	}
}
