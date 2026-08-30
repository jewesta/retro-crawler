package com.retrocrawler.mcp;

import java.util.Objects;

/** The safe, logical identity of one archive registered with RetroCrawler. */
public record ArchiveSummary(String id, String name) {

	public ArchiveSummary {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(name, "name");
	}
}
