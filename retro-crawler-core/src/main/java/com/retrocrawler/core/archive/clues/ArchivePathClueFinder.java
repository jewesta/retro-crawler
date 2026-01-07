package com.retrocrawler.core.archive.clues;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.retrocrawler.core.annotation.RetroArchive;
import com.retrocrawler.core.archive.ArchivePath;
import com.retrocrawler.core.util.Monitor;
import com.retrocrawler.core.util.Reflection;

public class ArchivePathClueFinder {

	private final PathNameClueFinder folderNameClueFinder;

	private final List<FileContentClueFinder> fileContentClueFinders;

	private final List<FileNameClueFinder> fileNameClueFinders;

	public ArchivePathClueFinder(final PathNameClueFinder folderNameClueFinder,
			final List<FileContentClueFinder> fileContentClueFinders,
			final List<FileNameClueFinder> fileNameClueFinders) {
		this.folderNameClueFinder = folderNameClueFinder;
		this.fileContentClueFinders = Objects.requireNonNullElse(fileContentClueFinders, List.of());
		this.fileNameClueFinders = Objects.requireNonNullElse(fileNameClueFinders, List.of());
		if (folderNameClueFinder == null && fileContentClueFinders.isEmpty() && fileNameClueFinders.isEmpty()) {
			throw new IllegalArgumentException("Require at least one clue finder.");
		}
	}

	private static Set<Clue> mergeAndAssertUnique(final Set<Clue> existing, final Set<Clue> incoming) {
		// Short-circuit
		if (incoming.isEmpty()) {
			return existing;
		}
		// We have merging to do
		final Map<String, Clue> byKey = new HashMap<>();

		for (final Clue clue : existing) {
			byKey.put(clue.getKey(), clue);
		}

		final Set<String> duplicates = new HashSet<>();

		for (final Clue clue : incoming) {
			final Clue previous = byKey.putIfAbsent(clue.getKey(), clue);
			if (previous != null) {
				duplicates.add(clue.getKey());
			}
		}

		if (!duplicates.isEmpty()) {
			throw new DuplicateClueException("Duplicate attribute(s) [" + String.join(", ", duplicates) + "]");
		}

		return new HashSet<>(byKey.values());
	}

	private static Set<Clue> from(final FileContentClueFinder finder, final Path file) {
		try (InputStream in = Files.newInputStream(file)) {
			return finder.find(in);
		} catch (final IOException e) {
			// TODO Error handling via Monitor
			throw new ClueFileIOException(e);
		}
	}

	public Set<Clue> find(final ArchivePath node, final Monitor monitor) {
		Set<Clue> clues;
		final String folderName = node.path().getFileName().toString();
		if (folderNameClueFinder != null) {
			clues = folderNameClueFinder.find(folderName);
		} else {
			clues = new HashSet<>();
		}
		/*
		 * For the file related clue finders we need to get regular files only, omitting
		 * folders.
		 */
		final List<Path> files = node.children().stream().filter(Files::isRegularFile).toList();
		if (files.isEmpty()) {
			/* No files. Nothing to get further clues from. */
			return clues;
		}
		/*
		 * For all file content clue finders we check for each file if the finder is
		 * compatible and if yes invoke the search.
		 */
		if (!fileContentClueFinders.isEmpty()) {
			for (final Path file : files) {
				final String fileName = file.getFileName().toString();
				for (final FileContentClueFinder finder : fileContentClueFinders) {
					if (!finder.matches(fileName)) {
						continue;
					}
					final Set<Clue> fileContentClues = from(finder, file);
					clues = mergeAndAssertUnique(clues, fileContentClues);
				}
			}
		}
		/*
		 * For the file name clue finders it is the other way around: Each finder we
		 * invoke with the total list of available files.
		 */
		for (final FileNameClueFinder finder : fileNameClueFinders) {
			final Set<Clue> fileNameClues = finder.find(files);
			clues = mergeAndAssertUnique(clues, fileNameClues);
		}
		return clues;
	}

	public static ArchivePathClueFinder of(final RetroArchive.LookAt lookAt) {
		Objects.requireNonNull(lookAt, "lookAt");

		final PathNameClueFinder folderNameClueFinder;
		if (BlindPathNameClueFinder.class.equals(lookAt.pathName())) {
			folderNameClueFinder = null;
		} else {
			folderNameClueFinder = Reflection.newInstance(lookAt.pathName());
		}

		final List<FileContentClueFinder> fileContentClueFinders = List.of(lookAt.fileContents()).stream()
				.map(Reflection::newInstance).map(FileContentClueFinder.class::cast).toList();

		final List<FileNameClueFinder> fileNameClueFinders = List.of(lookAt.fileNames()).stream()
				.map(Reflection::newInstance).map(FileNameClueFinder.class::cast).toList();

		// Let the constructor enforce that at least one finder is present.
		return new ArchivePathClueFinder(folderNameClueFinder, fileContentClueFinders, fileNameClueFinders);
	}

}
