package com.retrocrawler.core.crawl;

/** Lifecycle state of an asynchronous crawl operation. */
public enum CrawlOperationState {

	RUNNING,
	CANCELLING,
	SUCCEEDED,
	CANCELLED,
	FAILED
}
