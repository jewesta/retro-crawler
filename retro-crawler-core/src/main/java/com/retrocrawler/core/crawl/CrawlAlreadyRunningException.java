package com.retrocrawler.core.crawl;

import java.util.Objects;

import com.retrocrawler.core.util.RetroCrawlerException;

/** Rejects a crawl while another asynchronous crawl is still active. */
public final class CrawlAlreadyRunningException extends RetroCrawlerException {

	private static final long serialVersionUID = 1L;

	private final CrawlOperationId activeOperationId;

	public CrawlAlreadyRunningException(final CrawlOperationId activeOperationId) {
		super("A crawl operation is already active: " + activeOperationId);
		this.activeOperationId = Objects.requireNonNull(activeOperationId, "activeOperationId");
	}

	public CrawlOperationId activeOperationId() {
		return activeOperationId;
	}
}
