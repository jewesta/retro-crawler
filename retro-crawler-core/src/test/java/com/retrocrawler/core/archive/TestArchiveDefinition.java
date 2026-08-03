package com.retrocrawler.core.archive;

import java.util.List;

import com.retrocrawler.core.archive.clues.ArchivePathClueFinder;
import com.retrocrawler.core.archive.filter.ArchivePathFilter;

record TestArchiveDefinition(ArchiveDescriptor archiveDescriptor,
		ArchivePathClueFinder archivePathClueFinder,
		List<ArchivePathFilter> pathFilters) implements ArchiveDefinition {

	TestArchiveDefinition(final ArchiveDescriptor archiveDescriptor,
			final ArchivePathClueFinder archivePathClueFinder) {
		this(archiveDescriptor, archivePathClueFinder, List.of());
	}
}
