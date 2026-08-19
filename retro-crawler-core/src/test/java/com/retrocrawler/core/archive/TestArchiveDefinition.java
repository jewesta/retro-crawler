package com.retrocrawler.core.archive;

import java.util.List;

import com.retrocrawler.core.archive.clues.ClueFinder;
import com.retrocrawler.core.archive.filter.ArchivePathFilter;

record TestArchiveDefinition(ArchiveDescriptor archiveDescriptor, List<ClueFinder> clueFinders,
		List<ArchivePathFilter> pathFilters) implements ArchiveDefinition {

	TestArchiveDefinition(final ArchiveDescriptor archiveDescriptor, final ClueFinder clueFinder) {
		this(archiveDescriptor, List.of(clueFinder), List.of());
	}

	TestArchiveDefinition(final ArchiveDescriptor archiveDescriptor, final List<ClueFinder> clueFinders) {
		this(archiveDescriptor, clueFinders, List.of());
	}

	TestArchiveDefinition(final ArchiveDescriptor archiveDescriptor, final ClueFinder clueFinder,
			final List<ArchivePathFilter> pathFilters) {
		this(archiveDescriptor, List.of(clueFinder), pathFilters);
	}
}
