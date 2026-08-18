package com.retrocrawler.core.gear.parser;

import java.util.Objects;

import com.retrocrawler.core.Configuration;
import com.retrocrawler.core.archive.ArtifactLocation;

/**
 * Collection configuration and artifact location available while a raw clue is
 * interpreted.
 */
public record ParseContext(Configuration config, ArtifactLocation artifactLocation) {

	public ParseContext {
		config = Objects.requireNonNull(config, "config");
		artifactLocation = Objects.requireNonNull(artifactLocation, "artifactLocation");
	}
}
