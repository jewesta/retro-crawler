package com.retrocrawler.core.archive.clues;

import com.retrocrawler.core.util.RetroCrawlerException;

@SuppressWarnings("serial")
public class ClueFileIOException extends RetroCrawlerException {

	public ClueFileIOException() {
		super();
	}

	public ClueFileIOException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public ClueFileIOException(final String message) {
		super(message);
	}

	public ClueFileIOException(final Throwable cause) {
		super(cause);
	}

}
