package com.retrocrawler.core.archive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ReindexScopeTest {

	@Test
	void representsNoReindexAndCompleteReindexWithoutPaths() {
		assertEquals(ReindexScope.Kind.NONE, ReindexScope.none().kind());
		assertEquals(List.of(), ReindexScope.none().paths());
		assertEquals(ReindexScope.Kind.ALL, ReindexScope.all().kind());
		assertEquals(List.of(), ReindexScope.all().paths());
	}

	@Test
	void copiesSubtreePaths() {
		final List<Path> paths = new ArrayList<>(List.of(Path.of("one"), Path.of("two")));

		final ReindexScope scope = ReindexScope.subtrees(paths);
		paths.clear();

		assertEquals(ReindexScope.Kind.SUBTREES, scope.kind());
		assertEquals(List.of(Path.of("one"), Path.of("two")), scope.paths());
	}

	@Test
	void requiresAtLeastOneSubtree() {
		assertThrows(IllegalArgumentException.class, () -> ReindexScope.subtrees(List.of()));
	}
}
