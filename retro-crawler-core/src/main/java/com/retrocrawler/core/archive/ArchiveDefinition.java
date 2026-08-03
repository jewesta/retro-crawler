package com.retrocrawler.core.archive;

import java.util.List;

import com.retrocrawler.core.archive.clues.ArchivePathClueFinder;
import com.retrocrawler.core.archive.filter.ArchivePathFilter;

/**
 * Defines the filesystem archive from which a crawler extracts clues.
 */
public interface ArchiveDefinition {

	ArchiveDescriptor archiveDescriptor();

	ArchivePathClueFinder archivePathClueFinder();

	List<ArchivePathFilter> pathFilters();
}
