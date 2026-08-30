package com.retrocrawler.core.crawl;

import java.util.Objects;
import java.util.UUID;

/** Opaque identity of one asynchronous crawl operation. */
public record CrawlOperationId(String value) {

	public CrawlOperationId {
		value = Objects.requireNonNull(value, "value").trim();
		if (value.isEmpty()) {
			throw new IllegalArgumentException("Crawl operation ID must not be empty.");
		}
	}

	static CrawlOperationId create() {
		return new CrawlOperationId(UUID.randomUUID().toString());
	}

	@Override
	public String toString() {
		return value;
	}
}
