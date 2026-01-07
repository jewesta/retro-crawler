package com.retrocrawler.core.archive.clues;

import com.retrocrawler.core.util.RetroCrawlerException;

@SuppressWarnings("serial")
public class DuplicateClueException extends RetroCrawlerException {

	public DuplicateClueException() {
		super();
	}

	public DuplicateClueException(String message, Throwable cause) {
		super(message, cause);
	}

	public DuplicateClueException(String message) {
		super(message);
	}

	public DuplicateClueException(Throwable cause) {
		super(cause);
	}

}
