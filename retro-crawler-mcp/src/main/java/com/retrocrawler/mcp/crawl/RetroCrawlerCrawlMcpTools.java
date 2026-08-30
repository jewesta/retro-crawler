package com.retrocrawler.mcp.crawl;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;

import com.retrocrawler.core.RetroCrawler;
import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.archive.ReindexScope;
import com.retrocrawler.core.crawl.CrawlOperation;
import com.retrocrawler.core.crawl.CrawlOperationId;
import com.retrocrawler.core.crawl.CrawlOperationService;

/**
 * MCP crawl-control tools backed by the shared asynchronous operation service.
 */
public final class RetroCrawlerCrawlMcpTools {

	public static final int MAXIMUM_ARCHIVE_SELECTIONS = 100;

	private static final int MAXIMUM_OPERATION_ID_LENGTH = 200;

	private final RetroCrawler retroCrawler;

	private final CrawlOperationService operations;

	private final Map<String, ArchiveId> archiveIds;

	public RetroCrawlerCrawlMcpTools(final RetroCrawler retroCrawler, final CrawlOperationService operations) {
		this.retroCrawler = Objects.requireNonNull(retroCrawler, "retroCrawler");
		this.operations = Objects.requireNonNull(operations, "operations");
		this.archiveIds = archiveIds(retroCrawler.archives());
	}

	@McpTool(name = "start_crawl", title = "Start a RetroCrawler crawl",
			description = "Start an asynchronous physical crawl and return immediately with its operation status. "
					+ "Use logical archive IDs from list_archives; omit them to crawl every archive.",
			generateOutputSchema = true, annotations = @McpTool.McpAnnotations(readOnlyHint = false,
					destructiveHint = false, idempotentHint = false, openWorldHint = false))
	public CrawlStatus startCrawl(@McpToolParam(required = false,
			description = "Logical archive IDs from list_archives. Omit or use an empty list for every archive.") final List<String> archiveIds) {
		return CrawlStatusMapper.from(operations.start(scope(archiveIds)));
	}

	@McpTool(name = "get_crawl", title = "Get RetroCrawler crawl status",
			description = "Get the retained status, progress, timing, and bounded failures for one crawl operation. "
					+ "finishedAt is empty and remainingMillis is -1 while those values are unavailable.",
			generateOutputSchema = true, annotations = @McpTool.McpAnnotations(readOnlyHint = true,
					destructiveHint = false, idempotentHint = true, openWorldHint = false))
	public CrawlStatus getCrawl(
			@McpToolParam(description = "Operation ID returned by start_crawl.") final String operationId) {
		final CrawlOperationId id = operationId(operationId);
		final CrawlOperation operation = operations.operation(id)
				.orElseThrow(() -> new IllegalArgumentException("Unknown crawl operation: " + id));
		return CrawlStatusMapper.from(operation);
	}

	@McpTool(name = "cancel_crawl", title = "Cancel a RetroCrawler crawl",
			description = "Request cooperative cancellation of one retained crawl operation.",
			generateOutputSchema = true, annotations = @McpTool.McpAnnotations(readOnlyHint = false,
					destructiveHint = false, idempotentHint = true, openWorldHint = false))
	public CrawlStatus cancelCrawl(
			@McpToolParam(description = "Operation ID returned by start_crawl.") final String operationId) {
		return CrawlStatusMapper.from(operations.cancel(operationId(operationId)));
	}

	private ReindexScope scope(final List<String> selectedArchiveIds) {
		if (selectedArchiveIds == null || selectedArchiveIds.isEmpty()) {
			return ReindexScope.all();
		}
		if (selectedArchiveIds.size() > MAXIMUM_ARCHIVE_SELECTIONS) {
			throw new IllegalArgumentException(
					"archiveIds must not contain more than " + MAXIMUM_ARCHIVE_SELECTIONS + " entries.");
		}
		final LinkedHashSet<String> unique = new LinkedHashSet<>();
		final List<ARI> roots = selectedArchiveIds.stream().map(value -> archiveRoot(value, unique)).toList();
		return ReindexScope.subtrees(roots);
	}

	private ARI archiveRoot(final String value, final LinkedHashSet<String> unique) {
		final String id = Objects.requireNonNull(value, "archiveIds must not contain null");
		if (!unique.add(id)) {
			throw new IllegalArgumentException("archiveIds must not contain duplicate ID: " + id);
		}
		final ArchiveId archiveId = archiveIds.get(id);
		if (archiveId == null) {
			throw new IllegalArgumentException("Unknown archive id: " + id);
		}
		return ARI.of(retroCrawler.collectionId(), archiveId, Path.of(""));
	}

	private static CrawlOperationId operationId(final String value) {
		final String id = Objects.requireNonNull(value, "operationId").trim();
		if (id.length() > MAXIMUM_OPERATION_ID_LENGTH) {
			throw new IllegalArgumentException(
					"operationId must not exceed " + MAXIMUM_OPERATION_ID_LENGTH + " characters.");
		}
		return new CrawlOperationId(id);
	}

	private static Map<String, ArchiveId> archiveIds(final List<ArchiveDescriptor> archives) {
		final Map<String, ArchiveId> result = new LinkedHashMap<>();
		for (final ArchiveDescriptor archive : archives) {
			final ArchiveId previous = result.putIfAbsent(archive.id().value(), archive.id());
			if (previous != null) {
				throw new IllegalStateException("Several archives map to MCP archive id '" + archive.id() + "'.");
			}
		}
		return Map.copyOf(result);
	}
}
