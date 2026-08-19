package com.retrocrawler.core.gear.parser;

import java.util.Objects;

import com.retrocrawler.core.Configuration;
import com.retrocrawler.core.archive.ARI;

/**
 * Collection configuration and authoritative artifact identity available while
 * a raw clue is interpreted.
 */
public record ParseContext(Configuration config, ARI source) {

	public ParseContext {
		config = Objects.requireNonNull(config, "config");
		source = Objects.requireNonNull(source, "source");
	}
}
