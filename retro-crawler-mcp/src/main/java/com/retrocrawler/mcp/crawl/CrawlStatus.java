package com.retrocrawler.mcp.crawl;

import java.util.List;
import java.util.Objects;

/** Bounded MCP status for one asynchronous crawl operation. */
public record CrawlStatus(String id, CrawlScope scope, String state, boolean active, String startedAt,
		String finishedAt, CrawlProgress progress, List<CrawlFailure> failures, boolean failuresTruncated) {

	public CrawlStatus {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(scope, "scope");
		Objects.requireNonNull(state, "state");
		Objects.requireNonNull(startedAt, "startedAt");
		Objects.requireNonNull(finishedAt, "finishedAt");
		Objects.requireNonNull(progress, "progress");
		failures = List.copyOf(Objects.requireNonNull(failures, "failures"));
	}
}
