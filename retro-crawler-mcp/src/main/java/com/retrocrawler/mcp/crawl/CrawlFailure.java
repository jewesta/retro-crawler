package com.retrocrawler.mcp.crawl;

import java.util.Objects;

/** One bounded failure reported by a crawl operation. */
public record CrawlFailure(String type, String message) {

	public CrawlFailure {
		Objects.requireNonNull(type, "type");
		Objects.requireNonNull(message, "message");
	}
}
