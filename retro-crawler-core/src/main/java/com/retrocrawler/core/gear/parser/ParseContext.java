package com.retrocrawler.core.gear.parser;

import java.util.Objects;

import com.retrocrawler.core.Configuration;
import com.retrocrawler.core.archive.Node;

/**
 * Collection configuration and current archive node available while a raw clue
 * is interpreted.
 */
public record ParseContext(Configuration config, Node currentNode) {

	public ParseContext {
		config = Objects.requireNonNull(config, "config");
		currentNode = Objects.requireNonNull(currentNode, "currentNode");
	}
}
