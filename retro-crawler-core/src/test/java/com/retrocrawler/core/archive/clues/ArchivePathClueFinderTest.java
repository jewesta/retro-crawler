package com.retrocrawler.core.archive.clues;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.retrocrawler.core.archive.ArchivePath;
import com.retrocrawler.core.util.Monitor;

class ArchivePathClueFinderTest {

	private static final Monitor SILENT_MONITOR = new Monitor(message -> {
		// No progress output required in tests.
	});

	@TempDir
	private Path folder;

	@Test
	void collapsesCorroboratingCluesFromDifferentFinders() throws IOException {
		final Path file = Files.createFile(folder.resolve("evidence.txt"));
		final ArchivePathClueFinder finder = new ArchivePathClueFinder(
				name -> Set.of(Clue.of("bus", "AGP")),
				List.of(),
				List.of(files -> Set.of(Clue.of("bus", "AGP"))));

		final Set<Clue> clues = finder.find(new ArchivePath(folder, List.of(file)), SILENT_MONITOR);

		assertEquals(Set.of("AGP"), clue(clues, "bus").getValue());
	}

	@Test
	void retainsConflictingValuesUnderTheirSemanticKey() throws IOException {
		final Path file = Files.createFile(folder.resolve("evidence.txt"));
		final ArchivePathClueFinder finder = new ArchivePathClueFinder(
				name -> Set.of(Clue.of("bus", "AGP")),
				List.of(),
				List.of(files -> Set.of(Clue.of("bus", "PCI"))));

		final Set<Clue> clues = finder.find(new ArchivePath(folder, List.of(file)), SILENT_MONITOR);

		assertEquals(Set.of("AGP", "PCI"), clue(clues, "bus").getValue());
	}

	@Test
	void alsoMergesRepeatedKeysReturnedByOneFinder() {
		final ArchivePathClueFinder finder = new ArchivePathClueFinder(
				name -> Set.of(Clue.of("bus", "AGP"), Clue.of("bus", "PCI")),
				List.of(),
				List.of());

		final Set<Clue> clues = finder.find(new ArchivePath(folder, List.of()), SILENT_MONITOR);

		assertEquals(Set.of("AGP", "PCI"), clue(clues, "bus").getValue());
	}

	private static Clue clue(final Set<Clue> clues, final String key) {
		return clues.stream().filter(candidate -> key.equals(candidate.getKey())).findFirst().orElseThrow();
	}
}
