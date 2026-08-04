package com.retrocrawler.core.archive.source;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemArchiveSourceTest {

	@TempDir
	private Path root;

	@Test
	void classifiesDirectEntriesAndProvidesFileContent() throws IOException {
		final Path folder = Files.createDirectory(root.resolve("folder"));
		final Path file = Files.writeString(root.resolve("evidence.txt"), "observed");

		try (ArchiveSession session = new FileSystemArchiveSource().open(root)) {
			final ArchiveListing listing = session.list(session.root());

			assertEquals(Set.of(folder), paths(listing.folders()));
			assertEquals(Set.of(file), paths(listing.files()));
			assertEquals(Optional.of("observed"), session.access(listing.files().getFirst(),
					content -> new String(content.readAllBytes(), StandardCharsets.UTF_8)));
		}
	}

	@Test
	void alwaysClosesContentAfterACompletedAccessor() throws IOException {
		Files.writeString(root.resolve("evidence.txt"), "observed");
		final InputStream[] inspected = new InputStream[1];

		try (ArchiveSession session = new FileSystemArchiveSource().open(root)) {
			final ArchiveFile file = session.list(session.root()).files().getFirst();
			session.access(file, content -> {
				inspected[0] = content;
				return content.read();
			});
		}

		assertThrows(IOException.class, () -> inspected[0].read());
	}

	@Test
	void alwaysClosesContentAfterAFailedAccessor() throws IOException {
		Files.writeString(root.resolve("evidence.txt"), "observed");
		final InputStream[] inspected = new InputStream[1];

		try (ArchiveSession session = new FileSystemArchiveSource().open(root)) {
			final ArchiveFile file = session.list(session.root()).files().getFirst();
			assertThrows(IOException.class, () -> session.access(file, content -> {
				inspected[0] = content;
				throw new IOException("Inspection failed.");
			}));
		}

		assertThrows(IOException.class, () -> inspected[0].read());
	}

	@Test
	void rejectsHandlesFromAnotherSessionAndOperationsAfterClose() throws IOException {
		Files.createFile(root.resolve("evidence.txt"));
		final ArchiveSession first = new FileSystemArchiveSource().open(root);
		final ArchiveSession second = new FileSystemArchiveSource().open(root);
		final ArchiveFile firstFile = first.list(first.root()).files().getFirst();

		assertThrows(IllegalArgumentException.class, () -> second.access(firstFile, InputStream::read));

		first.close();
		assertThrows(IOException.class, first::root);
		second.close();
	}

	private static Set<Path> paths(final Iterable<? extends ArchiveEntry> entries) {
		final Set<Path> paths = new HashSet<>();
		entries.forEach(entry -> paths.add(entry.path()));
		return Set.copyOf(paths);
	}
}
