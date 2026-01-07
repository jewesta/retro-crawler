package com.retrocrawler.core.archive.clues;

import com.retrocrawler.core.util.RetroCrawlerException;

@SuppressWarnings("serial")
public class ClueFileIOException extends RetroCrawlerException {

	public ClueFileIOException() {
		super();
	}

	public ClueFileIOException(String message, Throwable cause) {
		super(message, cause);
	}

	public ClueFileIOException(String message) {
		super(message);
	}

	public ClueFileIOException(Throwable cause) {
		super(cause);
	}

}
