package com.retrocrawler.mcp.search;

import java.util.List;
import java.util.Objects;

/** A resolved Fact included in a bounded Gear search result. */
public record SearchFact(String filterId, List<String> values, String confidence, boolean valuesTruncated) {

	public SearchFact {
		Objects.requireNonNull(filterId, "filterId");
		Objects.requireNonNull(values, "values");
		values = List.copyOf(values);
		Objects.requireNonNull(confidence, "confidence");
	}
}
