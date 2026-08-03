package com.retrocrawler.core.gear.parser;

import com.retrocrawler.core.util.RetroCrawlerException;

@SuppressWarnings("serial")
public class AttributeValueParseException extends RetroCrawlerException {

	public AttributeValueParseException() {
		super();
	}

	public AttributeValueParseException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public AttributeValueParseException(final String message) {
		super(message);
	}

	public AttributeValueParseException(final Throwable cause) {
		super(cause);
	}

}
