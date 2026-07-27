package com.retrocrawler.core.progress;

import com.retrocrawler.core.util.RetroCrawlerException;

public final class ProgressCancelledException extends RetroCrawlerException {

	private static final long serialVersionUID = 1L;

	public ProgressCancelledException(final String message) {
		super(message);
	}
}
