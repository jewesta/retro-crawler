package com.retrocrawler.mcp.search;

import java.util.List;
import java.util.Objects;

/** One bounded page from a deterministic Gear search. */
public record SearchGearResult(String collectionId, long total, int offset, int limit, boolean hasMore,
		List<SearchGearHit> gear) {

	public SearchGearResult {
		Objects.requireNonNull(collectionId, "collectionId");
		if (total < 0) {
			throw new IllegalArgumentException("total must not be negative.");
		}
		if (offset < 0) {
			throw new IllegalArgumentException("offset must not be negative.");
		}
		if (limit < 1) {
			throw new IllegalArgumentException("limit must be positive.");
		}
		Objects.requireNonNull(gear, "gear");
		gear = List.copyOf(gear);
	}
}
