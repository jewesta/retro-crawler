package com.retrocrawler.core.archive;

import java.util.List;

import com.retrocrawler.core.archive.clues.ArchiveFolderClueFinder;
import com.retrocrawler.core.archive.filter.ArchivePathFilter;

record TestArchiveDefinition(ArchiveDescriptor archiveDescriptor, ArchiveFolderClueFinder archiveFolderClueFinder,
		List<ArchivePathFilter> pathFilters) implements ArchiveDefinition {

	TestArchiveDefinition(final ArchiveDescriptor archiveDescriptor,
			final ArchiveFolderClueFinder archiveFolderClueFinder) {
		this(archiveDescriptor, archiveFolderClueFinder, List.of());
	}
}
