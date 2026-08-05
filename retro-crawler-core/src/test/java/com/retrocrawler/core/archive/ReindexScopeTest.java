package com.retrocrawler.core.archive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ReindexScopeTest {

	private static final ArchiveId ARCHIVE_ID = ArchiveId.of("archive");

	@Test
	void representsNoReindexAndCompleteReindexWithoutPaths() {
		assertEquals(ReindexScope.Kind.NONE, ReindexScope.none().kind());
		assertEquals(List.of(), ReindexScope.none().subtrees());
		assertEquals(ReindexScope.Kind.ALL, ReindexScope.all().kind());
		assertEquals(List.of(), ReindexScope.all().subtrees());
	}

	@Test
	void copiesSubtreeAris() {
		final List<ARI> subtrees = new ArrayList<>(List.of(ari("one"), ari("two")));

		final ReindexScope scope = ReindexScope.subtrees(subtrees);
		subtrees.clear();

		assertEquals(ReindexScope.Kind.SUBTREES, scope.kind());
		assertEquals(List.of(ari("one"), ari("two")), scope.subtrees());
	}

	@Test
	void requiresAtLeastOneSubtree() {
		assertThrows(IllegalArgumentException.class, () -> ReindexScope.subtrees(List.of()));
	}

	private static ARI ari(final String path) {
		return ARI.of("collection", ARCHIVE_ID, java.nio.file.Path.of(path));
	}
}
