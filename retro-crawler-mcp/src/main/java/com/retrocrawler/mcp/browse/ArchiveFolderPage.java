package com.retrocrawler.mcp.browse;

import java.util.List;
import java.util.Objects;

/** One bounded page of direct children below an indexed archive folder. */
public record ArchiveFolderPage(ArchiveFolderSummary folder, long totalChildren, int offset, int limit, boolean hasMore,
		List<ArchiveFolderSummary> children) {

	public ArchiveFolderPage {
		Objects.requireNonNull(folder, "folder");
		if (totalChildren < 0) {
			throw new IllegalArgumentException("totalChildren must not be negative.");
		}
		if (offset < 0) {
			throw new IllegalArgumentException("offset must not be negative.");
		}
		if (limit < 1) {
			throw new IllegalArgumentException("limit must be positive.");
		}
		children = List.copyOf(Objects.requireNonNull(children, "children"));
		if (children.size() > limit) {
			throw new IllegalArgumentException("children must not exceed limit.");
		}
	}
}
