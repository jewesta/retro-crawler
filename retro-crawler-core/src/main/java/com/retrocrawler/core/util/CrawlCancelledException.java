package com.retrocrawler.core.util;

public final class CrawlCancelledException extends RetroCrawlerException {

	private static final long serialVersionUID = 1L;

	public CrawlCancelledException(final String message) {
		super(message);
	}
}
