package com.retrocrawler.core.archive;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One classified snapshot of a folder's direct entries.
 */
record FolderListing(List<Path> entries, List<Path> folders, List<Path> files) {

	FolderListing {
		entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
		folders = List.copyOf(Objects.requireNonNull(folders, "folders"));
		files = List.copyOf(Objects.requireNonNull(files, "files"));
	}

	static FolderListing from(final List<Path> entries) {
		Objects.requireNonNull(entries, "entries");
		final List<Path> folders = new ArrayList<>();
		final List<Path> files = new ArrayList<>();
		for (final Path entry : entries) {
			if (Files.isDirectory(entry)) {
				folders.add(entry);
			} else if (Files.isRegularFile(entry)) {
				files.add(entry);
			}
		}
		return new FolderListing(entries, folders, files);
	}
}
