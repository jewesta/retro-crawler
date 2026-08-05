package com.retrocrawler.core.archive.clues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.progress.Progressor;

class ArchivePathClueFinderTest {

	@Test
	void rekeysCollidingAnonymousCluesInsteadOfMergingTheirValues() {
		final Clue first = new Clue("_deadbeef", Set.of("first"));
		final Clue second = new Clue("_deadbeef", Set.of("second"));
		final ArchivePathClueFinder finder = new ArchivePathClueFinder(ignored -> Set.of(), List.of(), List.of());

		final Set<Clue> merged = finder.enrich(Set.of(first, second), emptyFolder(), new Progressor());

		assertEquals(2, merged.size());
		assertTrue(merged.stream().allMatch(clue -> clue.value().size() == 1));
		assertEquals(Set.of("first", "second"),
				merged.stream().flatMap(clue -> clue.value().stream()).collect(Collectors.toSet()));
		final Set<String> keys = merged.stream().map(Clue::key).collect(Collectors.toSet());
		assertEquals(2, keys.size());
		assertTrue(keys.contains("_deadbeef"));
		assertTrue(keys.stream().allMatch(key -> key.matches("_[a-z0-9]{8}")));
	}

	private static ArchiveFolderView emptyFolder() {
		return new ArchiveFolderView() {

			@Override
			public String name() {
				return "folder";
			}

			@Override
			public List<ArchiveFolderView> folders() {
				return List.of();
			}

			@Override
			public List<ArchiveFileView> files() {
				return List.of();
			}
		};
	}
}
