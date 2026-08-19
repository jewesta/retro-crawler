package com.retrocrawler.core.archive;

import java.util.List;

import com.retrocrawler.core.archive.clues.ClueFinder;
import com.retrocrawler.core.archive.filter.ArchivePathFilter;

record TestArchiveDefinition(String collectionId, ArchiveDescriptor archiveDescriptor, List<ClueFinder> clueFinders,
		List<ArchivePathFilter> pathFilters) implements ArchiveDefinition {

	private static final String TEST_COLLECTION = "test_collection";

	TestArchiveDefinition(final ArchiveDescriptor archiveDescriptor, final ClueFinder clueFinder) {
		this(TEST_COLLECTION, archiveDescriptor, List.of(clueFinder), List.of());
	}

	TestArchiveDefinition(final ArchiveDescriptor archiveDescriptor, final List<ClueFinder> clueFinders) {
		this(TEST_COLLECTION, archiveDescriptor, clueFinders, List.of());
	}

	TestArchiveDefinition(final ArchiveDescriptor archiveDescriptor, final ClueFinder clueFinder,
			final List<ArchivePathFilter> pathFilters) {
		this(TEST_COLLECTION, archiveDescriptor, List.of(clueFinder), pathFilters);
	}
}
