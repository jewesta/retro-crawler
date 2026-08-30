package com.retrocrawler.mcp.crawl;

import java.util.List;
import java.util.Objects;

/** The logical collection scope selected for one crawl. */
public record CrawlScope(String kind, List<String> subtrees) {

	public CrawlScope {
		Objects.requireNonNull(kind, "kind");
		subtrees = List.copyOf(Objects.requireNonNull(subtrees, "subtrees"));
	}
}
