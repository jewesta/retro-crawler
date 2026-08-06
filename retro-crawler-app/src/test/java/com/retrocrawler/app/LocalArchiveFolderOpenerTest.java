package com.retrocrawler.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalArchiveFolderOpenerTest {

	@TempDir
	private Path temporaryDirectory;

	@Test
	void opensCanonicalFolderBelowArchiveRoot() throws IOException {
		final Path archiveRoot = Files.createDirectory(temporaryDirectory.resolve("archive"));
		final Path sourcePath = Files.createDirectories(archiveRoot.resolve("shelf").resolve("gear"));
		final AtomicReference<Path> opened = new AtomicReference<>();
		final LocalArchiveFolderOpener opener = new LocalArchiveFolderOpener(opened::set);

		opener.open(sourcePath, archiveRoot);

		assertEquals(sourcePath.toRealPath(), opened.get());
	}

	@Test
	void refusesFolderOutsideTheArchiveRoot() throws IOException {
		final Path archiveRoot = Files.createDirectory(temporaryDirectory.resolve("archive"));
		final Path outside = Files.createDirectory(temporaryDirectory.resolve("outside"));
		final LocalArchiveFolderOpener opener = new LocalArchiveFolderOpener(path -> {
			throw new AssertionError("The open action must not run.");
		});

		assertThrows(IllegalArgumentException.class, () -> opener.open(outside, archiveRoot));
	}
}
