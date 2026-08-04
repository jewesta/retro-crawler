package com.retrocrawler.core.archive.source;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ZipArchiveSourceTest {

	@TempDir
	private Path temporaryDirectory;

	@Test
	void exposesDirectEntriesImplicitFoldersAndFileContent() throws IOException {
		final Path archive = createArchive(file("root.txt", "root evidence"),
				file("manuals/readme.txt", "nested evidence"), file("implicit/deep/evidence.txt", "deep evidence"),
				folder("empty/"));

		try (ArchiveSession session = new ZipArchiveSource().open(archive)) {
			final ArchiveListing root = session.list(session.root());

			assertEquals(Set.of(archive.resolve("manuals"), archive.resolve("implicit"), archive.resolve("empty")),
					paths(root.folders()));
			assertEquals(Set.of(archive.resolve("root.txt")), paths(root.files()));

			final ArchiveFolder manuals = folder(root, "manuals");
			final ArchiveFile readme = session.list(manuals).files().getFirst();
			assertEquals(archive.resolve("manuals/readme.txt"), readme.path());
			assertEquals(Optional.of("nested evidence"),
					session.access(readme, content -> new String(content.readAllBytes(), StandardCharsets.UTF_8)));

			final ArchiveFolder implicit = folder(root, "implicit");
			assertEquals(Set.of(archive.resolve("implicit/deep")), paths(session.list(implicit).folders()));
			assertEquals(new ArchiveListing(List.of(), List.of()), session.list(folder(root, "empty")));
		}
	}

	@Test
	void alwaysClosesContentAfterACompletedAccessor() throws IOException {
		final Path archive = createArchive(file("evidence.txt", "observed"));
		final InputStream[] inspected = new InputStream[1];

		try (ArchiveSession session = new ZipArchiveSource().open(archive)) {
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
		final Path archive = createArchive(file("evidence.txt", "observed"));
		final InputStream[] inspected = new InputStream[1];

		try (ArchiveSession session = new ZipArchiveSource().open(archive)) {
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
		final Path archive = createArchive(file("evidence.txt", "observed"));
		final ArchiveSession first = new ZipArchiveSource().open(archive);
		final ArchiveSession second = new ZipArchiveSource().open(archive);
		final ArchiveFile firstFile = first.list(first.root()).files().getFirst();

		assertThrows(IllegalArgumentException.class, () -> second.access(firstFile, InputStream::read));

		first.close();
		assertThrows(IOException.class, first::root);
		second.close();
	}

	@Test
	void rejectsEntriesThatEscapeTheArchiveRoot() throws IOException {
		final Path archive = createArchive(file("../outside.txt", "not evidence"));

		assertThrows(ZipException.class, () -> new ZipArchiveSource().open(archive));
	}

	@Test
	void rejectsFileAndFolderCollisions() throws IOException {
		final Path archive = createArchive(file("clash", "file"), file("clash/inside.txt", "nested"));

		assertThrows(ZipException.class, () -> new ZipArchiveSource().open(archive));
	}

	private Path createArchive(final Member... members) throws IOException {
		final Path archive = temporaryDirectory.resolve("archive.zip");
		try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archive))) {
			for (final Member member : members) {
				output.putNextEntry(new ZipEntry(member.name()));
				if (member.content() != null) {
					output.write(member.content());
				}
				output.closeEntry();
			}
		}
		return archive;
	}

	private static ArchiveFolder folder(final ArchiveListing listing, final String name) {
		return listing.folders().stream().filter(folder -> name.equals(folder.name())).findFirst().orElseThrow();
	}

	private static Set<Path> paths(final Iterable<? extends ArchiveEntry> entries) {
		final Set<Path> paths = new HashSet<>();
		entries.forEach(entry -> paths.add(entry.path()));
		return Set.copyOf(paths);
	}

	private static Member file(final String name, final String content) {
		return new Member(name, content.getBytes(StandardCharsets.UTF_8));
	}

	private static Member folder(final String name) {
		return new Member(name, null);
	}

	private record Member(String name, byte[] content) {
	}
}
