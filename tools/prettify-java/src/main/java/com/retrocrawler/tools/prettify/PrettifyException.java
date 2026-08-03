package com.retrocrawler.tools.prettify;

final class PrettifyException extends RuntimeException {

	PrettifyException(final String message) {
		super(message);
	}

	PrettifyException(final String message, final Throwable cause) {
		super(message, cause);
	}

}
