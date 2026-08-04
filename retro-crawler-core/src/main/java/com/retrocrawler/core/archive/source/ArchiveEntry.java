package com.retrocrawler.core.archive.source;

import java.nio.file.Path;

/**
 * A provider-owned entry in a rooted archive source.
 * <p>
 * The path identifies the entry within its source. Listings return rooted paths
 * for direct children of the requested folder. Paths are hierarchical but are
 * not necessarily accessible through the local filesystem.
 */
public interface ArchiveEntry {

	Path path();

	default String name() {
		final Path fileName = path().getFileName();
		return fileName == null ? path().toString() : fileName.toString();
	}
}
