package com.retrocrawler.mcp.search;

import java.util.List;
import java.util.Objects;

/** One Gear occurrence and its compact traceable summary. */
public record SearchGearHit(String source, String gearKind, List<SearchFact> facts, boolean factsTruncated,
		int resolutionIssueCount) {

	public SearchGearHit {
		Objects.requireNonNull(source, "source");
		Objects.requireNonNull(gearKind, "gearKind");
		Objects.requireNonNull(facts, "facts");
		facts = List.copyOf(facts);
		if (resolutionIssueCount < 0) {
			throw new IllegalArgumentException("resolutionIssueCount must not be negative.");
		}
	}
}
