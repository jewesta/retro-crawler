package com.retrocrawler.core.archive.clues;

import com.retrocrawler.core.util.RetroCrawlerException;

@SuppressWarnings("serial")
public class DuplicateClueException extends RetroCrawlerException {

	public DuplicateClueException() {
		super();
	}

	public DuplicateClueException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public DuplicateClueException(final String message) {
		super(message);
	}

	public DuplicateClueException(final Throwable cause) {
		super(cause);
	}

}
