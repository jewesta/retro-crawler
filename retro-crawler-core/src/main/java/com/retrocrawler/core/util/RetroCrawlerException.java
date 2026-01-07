package com.retrocrawler.core.util;

public class RetroCrawlerException extends RuntimeException {

	private static final long serialVersionUID = 6039350770927051598L;

	public RetroCrawlerException() {
	}

	public RetroCrawlerException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public RetroCrawlerException(final String message) {
		super(message);
	}

	public RetroCrawlerException(final Throwable cause) {
		super(cause);
	}

}
