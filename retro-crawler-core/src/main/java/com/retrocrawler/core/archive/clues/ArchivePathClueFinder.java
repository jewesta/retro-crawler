package com.retrocrawler.core.archive.clues;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.retrocrawler.core.annotation.RetroClues;
import com.retrocrawler.core.archive.source.ArchiveFile;
import com.retrocrawler.core.archive.source.ArchiveFolder;
import com.retrocrawler.core.archive.source.ArchiveSession;
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

	private static Set<Clue> mergeAndAssertUnique(final Set<Clue> existing, final Set<Clue> incoming) {
		final ClueAccumulator accumulator = new ClueAccumulator(existing);
		accumulator.addAll(incoming);
		return accumulator.clues();
	}

	private static Set<Clue> from(final FileContentClueFinder finder, final Path file) {
		try (InputStream in = Files.newInputStream(file)) {
			return finder.find(in);
		} catch (final IOException e) {
			// TODO Report the affected clue file through structured progress diagnostics.
			throw new ClueFileIOException(e);
		}
	}

	private static Optional<Set<Clue>> from(final FileContentClueFinder finder, final ArchiveSession session,
			final ArchiveFile file) {
		try {
			return session.access(file, finder::find);
		} catch (final IOException e) {
			throw new ClueFileIOException("Could not inspect clue file at: " + file.path(), e);
		}
	}

	/**
	 * Runs local clue finders against entries supplied by an archive source.
	 */
	public Set<Clue> find(final ArchiveFolder folder, final List<ArchiveFile> files, final ArchiveSession session,
			final Progressor progressor) {
		Objects.requireNonNull(folder, "folder");
		Objects.requireNonNull(files, "files");
		Objects.requireNonNull(session, "session");
		Objects.requireNonNull(progressor, "progressor");

		Set<Clue> clues;
		if (folderNameClueFinder == null) {
			clues = Set.of();
		} else {
			clues = mergeAndAssertUnique(Set.of(), folderNameClueFinder.find(folder.name()));
		}
		if (files.isEmpty()) {
			return clues;
		}

		if (!fileContentClueFinders.isEmpty()) {
			for (final ArchiveFile file : files) {
				for (final FileContentClueFinder finder : fileContentClueFinders) {
					if (!finder.matches(file.name())) {
						continue;
					}
					progressor.throwIfCancelled();
					final Optional<Set<Clue>> fileContentClues = from(finder, session, file);
					if (fileContentClues.isPresent()) {
						clues = mergeAndAssertUnique(clues, fileContentClues.get());
					}
				}
			}
		}

		final Path normalizedFolder = folder.path().normalize();
		final List<Path> relativeFiles = files.stream()
				.map(file -> normalizedFolder.relativize(file.path().normalize())).toList();
		for (final FileNameClueFinder finder : fileNameClueFinders) {
			clues = mergeAndAssertUnique(clues, finder.find(relativeFiles));
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
		Set<Clue> clues = mergeAndAssertUnique(Set.of(), localClues);
		for (final TreeClueFinder finder : treeClueFinders) {
			progressor.throwIfCancelled();
			clues = mergeAndAssertUnique(clues, finder.find(folder));
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
