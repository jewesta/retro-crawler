package com.retrocrawler.core.archive;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FolderListingTest {

	@TempDir
	private Path root;

	@Test
	void classifiesDirectEntriesOnceAndPreservesTheirOrder() throws IOException {
		final Path secondFile = Files.createFile(root.resolve("second.txt"));
		final Path folder = Files.createDirectory(root.resolve("folder"));
		final Path firstFile = Files.createFile(root.resolve("first.txt"));
		final Path missing = root.resolve("missing");
		final List<Path> entries = List.of(firstFile, folder, missing, secondFile);

		final FolderListing listing = FolderListing.from(entries);

		assertEquals(entries, listing.entries());
		assertEquals(List.of(folder), listing.folders());
		assertEquals(List.of(firstFile, secondFile), listing.files());
	}
}
