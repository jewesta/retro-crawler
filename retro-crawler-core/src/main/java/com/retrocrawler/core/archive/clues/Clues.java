package com.retrocrawler.core.archive.clues;

import java.util.Collection;
import java.util.Objects;

public record Clues(String key, Collection<String> values) {

	public Clues {
		Objects.requireNonNull(values, "values");
	}

	boolean isMystery() {
		return key == null;
	}

}
