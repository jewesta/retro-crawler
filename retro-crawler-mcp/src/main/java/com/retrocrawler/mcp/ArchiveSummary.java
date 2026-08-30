package com.retrocrawler.mcp;

import java.util.Objects;

/** The safe, logical identity of one archive registered with RetroCrawler. */
public record ArchiveSummary(String id, String name, String rootAri) {

	/**
	 * Retains source compatibility for callers that do not yet use browsing.
	 */
	public ArchiveSummary(final String id, final String name) {
		this(id, name, "");
	}

	public ArchiveSummary {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(name, "name");
		Objects.requireNonNull(rootAri, "rootAri");
	}
}
