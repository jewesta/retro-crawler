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

import com.retrocrawler.core.annotation.RetroClues;
import com.retrocrawler.core.archive.ArchivePath;
import com.retrocrawler.core.progress.Progressor;
import com.retrocrawler.core.util.Reflection;

public class ArchivePathClueFinder {

	private final FolderNameClueFinder folderNameClueFinder;

	private final List<FileContentClueFinder> fileContentClueFinders;

	private final List<FileNameClueFinder> fileNameClueFinders;

	private final List<TreeClueFinder> treeClueFinders;

	public ArchivePathClueFinder(final FolderNameClueFinder folderNameClueFinder,
			final List<FileContentClueFinder> fileContentClueFinders,
			final List<FileNameClueFinder> fileNameClueFinders) {
		this(folderNameClueFinder, fileContentClueFinders, fileNameClueFinders, List.of());
	}

	public ArchivePathClueFinder(final FolderNameClueFinder folderNameClueFinder,
			final List<FileContentClueFinder> fileContentClueFinders,
			final List<FileNameClueFinder> fileNameClueFinders, final List<TreeClueFinder> treeClueFinders) {
		this.folderNameClueFinder = folderNameClueFinder;
		this.fileContentClueFinders = Objects.requireNonNullElse(fileContentClueFinders, List.of());
		this.fileNameClueFinders = Objects.requireNonNullElse(fileNameClueFinders, List.of());
		this.treeClueFinders = Objects.requireNonNullElse(treeClueFinders, List.of());
		if (folderNameClueFinder == null && this.fileContentClueFinders.isEmpty() && this.fileNameClueFinders.isEmpty()
				&& this.treeClueFinders.isEmpty()) {
			throw new IllegalArgumentException("Require at least one clue finder.");
		}
	}

	private static Set<Clue> merge(final Set<Clue> existing, final Set<Clue> incoming) {
		// Short-circuit
		if (incoming.isEmpty()) {
			return existing;
		}
		// We have merging to do
		final Map<String, Clue> byKey = new HashMap<>();

		for (final Clue clue : existing) {
			byKey.put(clue.key(), clue);
		}

		for (final Clue clue : incoming) {
			final Clue previous = byKey.get(clue.key());
			if (previous == null) {
				byKey.put(clue.key(), clue);
				continue;
			}

			final Set<String> combinedValues = new HashSet<>(previous.value());
			combinedValues.addAll(clue.value());
			byKey.put(clue.key(), new Clue(clue.key(), Set.copyOf(combinedValues)));
		}

		return new HashSet<>(byKey.values());
	}

	private static Set<Clue> from(final FileContentClueFinder finder, final Path file) {
		try (InputStream in = Files.newInputStream(file)) {
			return finder.find(in);
		} catch (final IOException e) {
			// TODO Report the affected clue file through structured progress diagnostics.
			throw new ClueFileIOException(e);
		}
	}

	public Set<Clue> find(final ArchivePath node, final Progressor progressor) {
		final List<Path> files = node.children().stream().filter(Files::isRegularFile).toList();
		return find(node, files, progressor);
	}

	/**
	 * Runs local clue finders with a caller-supplied classification of the
	 * current folder's direct files.
	 */
	public Set<Clue> find(final ArchivePath node, final List<Path> files, final Progressor progressor) {
		Objects.requireNonNull(node, "node");
		Objects.requireNonNull(files, "files");
		Set<Clue> clues;
		final String folderName = node.path().getFileName().toString();
		if (folderNameClueFinder != null) {
			clues = merge(new HashSet<>(), folderNameClueFinder.find(folderName));
		} else {
			clues = new HashSet<>();
		}
		if (files.isEmpty()) {
			/* No files. Nothing to get further clues from. */
			return clues;
		}
		/*
		 * For all file content clue finders we check for each file if the
		 * finder is compatible and if yes invoke the search.
		 */
		if (!fileContentClueFinders.isEmpty()) {
			for (final Path file : files) {
				final String fileName = file.getFileName().toString();
				for (final FileContentClueFinder finder : fileContentClueFinders) {
					if (!finder.matches(fileName)) {
						continue;
					}
					final Set<Clue> fileContentClues = from(finder, file);
					clues = merge(clues, fileContentClues);
				}
			}
		}
		/*
		 * For the file name clue finders it is the other way around: Each
		 * finder we invoke with the total list of available files.
		 */
		final List<Path> relativeFiles = files.stream().map(node::relative).toList();
		for (final FileNameClueFinder finder : fileNameClueFinders) {
			final Set<Clue> fileNameClues = finder.find(relativeFiles);
			clues = merge(clues, fileNameClues);
		}
		return clues;
	}

	/**
	 * Enriches clues already found locally with observations from the
	 * configured post-order tree finders.
	 */
	public Set<Clue> enrich(final Set<Clue> localClues, final ArchiveFolderView folder, final Progressor progressor) {
		Objects.requireNonNull(localClues, "localClues");
		Objects.requireNonNull(folder, "folder");
		Set<Clue> clues = merge(new HashSet<>(), localClues);
		for (final TreeClueFinder finder : treeClueFinders) {
			progressor.throwIfCancelled();
			clues = merge(clues, finder.find(folder));
		}
		return clues;
	}

	public static ArchivePathClueFinder of(final RetroClues clues) {
		Objects.requireNonNull(clues, "clues");

		final FolderNameClueFinder folderNameClueFinder;
		if (BlindFolderNameClueFinder.class.equals(clues.fromFolderName())) {
			folderNameClueFinder = null;
		} else {
			folderNameClueFinder = Reflection.newInstance(clues.fromFolderName());
		}

		final List<FileContentClueFinder> fileContentClueFinders = List.of(clues.fromFileContents()).stream()
				.map(Reflection::newInstance).map(FileContentClueFinder.class::cast).toList();

		final List<FileNameClueFinder> fileNameClueFinders = List.of(clues.fromFileNames()).stream()
				.map(Reflection::newInstance).map(FileNameClueFinder.class::cast).toList();

		final List<TreeClueFinder> treeClueFinders = List.of(clues.fromFolderTrees()).stream()
				.map(Reflection::newInstance).map(TreeClueFinder.class::cast).toList();

		// Let the constructor enforce that at least one finder is present.
		return new ArchivePathClueFinder(folderNameClueFinder, fileContentClueFinders, fileNameClueFinders,
				treeClueFinders);
	}

}
