package com.retrocrawler.core.archive;

import com.retrocrawler.core.util.RetroCrawlerException;

public class RepositoryException extends RetroCrawlerException {

	private static final long serialVersionUID = -1895811881749928453L;

	public RepositoryException() {
	}

	public RepositoryException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public RepositoryException(final String message) {
		super(message);
	}

	public RepositoryException(final Throwable cause) {
		super(cause);
	}

}
