package com.retrocrawler.mcp.browse;

import java.util.List;
import java.util.Objects;

import com.retrocrawler.core.gear.GearType;

/** One indexed archive folder in a bounded browse result. */
public record ArchiveFolderSummary(String ari, String name, int childFolderCount, long subtreeGearCount,
		List<GearType> gearTypes, String crawlStartedAt, String observedAt) {

	public ArchiveFolderSummary {
		Objects.requireNonNull(ari, "ari");
		Objects.requireNonNull(name, "name");
		if (childFolderCount < 0) {
			throw new IllegalArgumentException("childFolderCount must not be negative.");
		}
		if (subtreeGearCount < 0) {
			throw new IllegalArgumentException("subtreeGearCount must not be negative.");
		}
		gearTypes = List.copyOf(Objects.requireNonNull(gearTypes, "gearTypes"));
		Objects.requireNonNull(crawlStartedAt, "crawlStartedAt");
		Objects.requireNonNull(observedAt, "observedAt");
	}
}
