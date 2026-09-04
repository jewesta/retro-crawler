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

	public static final int MAXIMUM_SUBTREE_SELECTIONS = 100;

	private static final int MAXIMUM_OPERATION_ID_LENGTH = 200;

	private static final int MAXIMUM_SUBTREE_ARI_LENGTH = 4_000;

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
					+ "Select either logical archive IDs from list_archives or canonical folder ARIs. "
					+ "Omit both selections to crawl every archive.",
			generateOutputSchema = true, annotations = @McpTool.McpAnnotations(readOnlyHint = false,
					destructiveHint = false, idempotentHint = false, openWorldHint = false))
	public CrawlStatus startCrawl(@McpToolParam(required = false,
			description = "Logical archive IDs from list_archives. Mutually exclusive with subtreeAris.") final List<String> archiveIds,
			@McpToolParam(required = false,
					description = "Canonical folder ARIs to re-crawl, including everything below each folder. Mutually exclusive with archiveIds.") final List<String> subtreeAris) {
		return CrawlStatusMapper.from(operations.start(scope(archiveIds, subtreeAris)));
	}

	/**
	 * Retains the original Java entry point while the MCP schema gains subtree
	 * selection.
	 */
	public CrawlStatus startCrawl(final List<String> archiveIds) {
		return startCrawl(archiveIds, null);
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

	private ReindexScope scope(final List<String> selectedArchiveIds, final List<String> selectedSubtreeAris) {
		final boolean archivesSelected = selectedArchiveIds != null && !selectedArchiveIds.isEmpty();
		final boolean subtreesSelected = selectedSubtreeAris != null && !selectedSubtreeAris.isEmpty();
		if (archivesSelected && subtreesSelected) {
			throw new IllegalArgumentException("archiveIds and subtreeAris are mutually exclusive.");
		}
		if (!archivesSelected && !subtreesSelected) {
			return ReindexScope.all();
		}
		if (subtreesSelected) {
			return ReindexScope.subtrees(subtrees(selectedSubtreeAris));
		}
		if (selectedArchiveIds.size() > MAXIMUM_ARCHIVE_SELECTIONS) {
			throw new IllegalArgumentException(
					"archiveIds must not contain more than " + MAXIMUM_ARCHIVE_SELECTIONS + " entries.");
		}
		final LinkedHashSet<String> unique = new LinkedHashSet<>();
		final List<ARI> roots = selectedArchiveIds.stream().map(value -> archiveRoot(value, unique)).toList();
		return ReindexScope.subtrees(roots);
	}

	private List<ARI> subtrees(final List<String> selectedSubtreeAris) {
		if (selectedSubtreeAris.size() > MAXIMUM_SUBTREE_SELECTIONS) {
			throw new IllegalArgumentException(
					"subtreeAris must not contain more than " + MAXIMUM_SUBTREE_SELECTIONS + " entries.");
		}
		final LinkedHashSet<ARI> unique = new LinkedHashSet<>();
		return selectedSubtreeAris.stream().map(value -> subtree(value, unique)).toList();
	}

	private ARI subtree(final String value, final LinkedHashSet<ARI> unique) {
		final String text = Objects.requireNonNull(value, "subtreeAris must not contain null");
		if (text.isBlank()) {
			throw new IllegalArgumentException("subtreeAris must not contain a blank ARI.");
		}
		if (text.length() > MAXIMUM_SUBTREE_ARI_LENGTH) {
			throw new IllegalArgumentException(
					"A subtree ARI must not exceed " + MAXIMUM_SUBTREE_ARI_LENGTH + " characters.");
		}
		final ARI subtree;
		try {
			subtree = ARI.parse(text);
		} catch (final IllegalArgumentException failure) {
			throw new IllegalArgumentException("Invalid subtree ARI: " + text, failure);
		}
		if (!retroCrawler.collectionId().equals(subtree.collectionId())) {
			throw new IllegalArgumentException("Subtree ARI belongs to collection '" + subtree.collectionId()
					+ "' instead of '" + retroCrawler.collectionId() + "': " + subtree);
		}
		if (!archiveIds.containsKey(subtree.archiveId().value())) {
			throw new IllegalArgumentException("Unknown archive id in subtree ARI: " + subtree.archiveId());
		}
		if (!unique.add(subtree)) {
			throw new IllegalArgumentException("subtreeAris must not contain duplicate ARI: " + subtree);
		}
		return subtree;
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
