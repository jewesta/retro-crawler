package com.retrocrawler.core.gear.parser;

import com.retrocrawler.core.util.RetroCrawlerException;

@SuppressWarnings("serial")
public class AttributeValueParseException extends RetroCrawlerException {

	public AttributeValueParseException() {
		super();
	}

	public AttributeValueParseException(String message, Throwable cause) {
		super(message, cause);
	}

	public AttributeValueParseException(String message) {
		super(message);
	}

	public AttributeValueParseException(Throwable cause) {
		super(cause);
	}

}
