package com.retrocrawler.core.archive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ArchiveRootsTest {

	@TempDir
	private Path temporaryDirectory;

	@Test
	void createsImmutableRootsFromVarargs() {
		final Path first = Path.of("/archive/first");
		final Path second = Path.of("/archive/second");

		final ArchiveRoots roots = ArchiveRoots.from(first, second);

		assertEquals(List.of(first, second), roots.getPaths());
		assertThrows(UnsupportedOperationException.class,
				() -> roots.getPaths().add(Path.of("/archive/third")));
	}

	@Test
	void defensivelyCopiesACollection() {
		final Path first = Path.of("/archive/first");
		final List<Path> source = new ArrayList<>(List.of(first));

		final ArchiveRoots roots = ArchiveRoots.from(source);
		source.add(Path.of("/archive/second"));

		assertEquals(List.of(first), roots.getPaths());
	}

	@Test
	void loadsOneTrimmedPathPerNonblankUtf8Line() throws IOException {
		final Path first = temporaryDirectory.resolve("First archive");
		final Path second = temporaryDirectory.resolve("Second archive");
		final Path pathFile = temporaryDirectory.resolve("archive-roots.txt");
		Files.writeString(pathFile, first + "\n\n  " + second + "  \n");

		final ArchiveRoots roots = ArchiveRoots.load(pathFile);

		assertEquals(List.of(first, second), roots.getPaths());
	}

	@Test
	void rejectsAnEmptyCollection() {
		final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
				() -> ArchiveRoots.from(List.of()));

		assertEquals("At least one archive root is required.", failure.getMessage());
	}

	@Test
	void rejectsAPathFileWithoutRoots() throws IOException {
		final Path pathFile = temporaryDirectory.resolve("empty-roots.txt");
		Files.writeString(pathFile, "\n \n");

		final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
				() -> ArchiveRoots.load(pathFile));

		assertEquals("At least one archive root is required.", failure.getMessage());
	}
}
