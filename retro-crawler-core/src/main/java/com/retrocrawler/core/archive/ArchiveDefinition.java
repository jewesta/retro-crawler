package com.retrocrawler.core.archive;

import java.nio.file.Path;
import java.util.List;

import com.retrocrawler.core.archive.clues.ClueFinder;
import com.retrocrawler.core.archive.filter.ArchivePathFilter;

/**
 * Defines the archive from which a crawler extracts clues. Runtime composition
 * selects the source that exposes its configured locations.
 */
public interface ArchiveDefinition {

	/** The collection namespace in which this archive is identified. */
	String collectionId();

	ArchiveDescriptor archiveDescriptor();

	/**
	 * Identifies one resource by its path relative to this archive's root.
	 */
	default ARI ariFrom(final Path resourcePath) {
		return ARI.of(collectionId(), archiveDescriptor().id(), resourcePath);
	}

	/** The clue finders applied to every candidate folder, in order. */
	List<ClueFinder> clueFinders();

	List<ArchivePathFilter> pathFilters();
}
