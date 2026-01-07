package com.retrocrawler.core.gear.parser;

import java.util.Objects;

import com.retrocrawler.core.archive.clues.Artifact;
import com.retrocrawler.core.archive.clues.Clue;

public record FieldContext(Class<?> fieldType, Clue clue, Artifact artifact) {

	public FieldContext {
		Objects.requireNonNull(fieldType, "fieldType");
		Objects.requireNonNull(clue, "clue");
		Objects.requireNonNull(artifact, "artifact");
	}

}