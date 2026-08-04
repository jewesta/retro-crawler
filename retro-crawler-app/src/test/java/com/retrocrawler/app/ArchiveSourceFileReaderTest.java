package com.retrocrawler.app;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.retrocrawler.core.archive.source.FileSystemArchiveSource;
import com.retrocrawler.core.archive.source.ZipArchiveSource;

class ArchiveSourceFileReaderTest {

	@TempDir
	private Path temporaryDirectory;

	@Test
	void readsAFileFromTheFilesystemProvider() throws IOException {
		final Path root = Files.createDirectory(temporaryDirectory.resolve("archive"));
		final Path file = Files.writeString(Files.createDirectory(root.resolve("gear")).resolve("front.jpeg"),
				"filesystem image");
		final ArchiveSourceFileReader reader = new ArchiveSourceFileReader(new FileSystemArchiveSource(),
				List.of(root));

		final Optional<byte[]> content = reader.read(file);

		assertTrue(content.isPresent());
		assertArrayEquals("filesystem image".getBytes(StandardCharsets.UTF_8), content.get());
	}

	@Test
	void readsAFileFromTheZipProvider() throws IOException {
		final Path archive = temporaryDirectory.resolve("archive.zip");
		try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archive))) {
			output.putNextEntry(new ZipEntry("gear/front.jpeg"));
			output.write("zip image".getBytes(StandardCharsets.UTF_8));
			output.closeEntry();
		}
		final ArchiveSourceFileReader reader = new ArchiveSourceFileReader(new ZipArchiveSource(), List.of(archive));

		final Optional<byte[]> content = reader.read(archive.resolve("gear/front.jpeg"));

		assertTrue(content.isPresent());
		assertArrayEquals("zip image".getBytes(StandardCharsets.UTF_8), content.get());
		assertTrue(Files.notExists(temporaryDirectory.resolve("gear")));
	}

	@Test
	void returnsEmptyWhenTheAddressDoesNotIdentifyAFile() throws IOException {
		final Path root = Files.createDirectory(temporaryDirectory.resolve("archive"));
		final ArchiveSourceFileReader reader = new ArchiveSourceFileReader(new FileSystemArchiveSource(),
				List.of(root));

		assertTrue(reader.read(root.resolve("missing.jpeg")).isEmpty());
	}

	@Test
	void rejectsAddressesOutsideItsConfiguredRoots() throws IOException {
		final Path root = Files.createDirectory(temporaryDirectory.resolve("archive"));
		final ArchiveSourceFileReader reader = new ArchiveSourceFileReader(new FileSystemArchiveSource(),
				List.of(root));

		assertThrows(IllegalArgumentException.class, () -> reader.read(temporaryDirectory.resolve("outside.jpeg")));
	}
}
