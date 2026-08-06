package com.retrocrawler.core.archive;

import java.util.List;

import com.retrocrawler.core.archive.clues.ArchiveFolderClueFinder;
import com.retrocrawler.core.archive.filter.ArchivePathFilter;

/**
 * Defines the archive from which a crawler extracts clues. Runtime composition
 * selects the source that exposes its configured locations.
 */
public interface ArchiveDefinition {

	ArchiveDescriptor archiveDescriptor();

	ArchiveFolderClueFinder archiveFolderClueFinder();

	List<ArchivePathFilter> pathFilters();
}
