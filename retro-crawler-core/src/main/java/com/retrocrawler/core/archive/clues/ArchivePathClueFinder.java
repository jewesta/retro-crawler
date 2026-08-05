package com.retrocrawler.core.archive.clues;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

	private static Optional<Clues> from(final FileContentClueFinder finder, final ArchiveSession session,
			final ArchiveFile file) {
		try {
			// TODO Report the affected clue file through structured progress diagnostics.
			return session.access(file, finder::find);
		} catch (final IOException e) {
			throw new ClueFileIOException("Could not inspect clue file at: " + file.path(), e);
		}
	}

	/**
	 * Runs local clue finders against entries supplied by an archive source.
	 */
	public Clues find(final ArchiveFolder folder, final List<ArchiveFile> files, final ArchiveSession session,
			final Progressor progressor) {
		Objects.requireNonNull(folder, "folder");
		Objects.requireNonNull(files, "files");
		Objects.requireNonNull(session, "session");
		Objects.requireNonNull(progressor, "progressor");

		/*
		 * One accumulator for every finder at this location. Each observation
		 * is checked once, as it arrives, against everything observed so far.
		 */
		final ClueAccumulator clues = Clues.accumulator();
		if (folderNameClueFinder != null) {
			clues.addAll(folderNameClueFinder.find(folder.name()));
		}
		if (files.isEmpty()) {
			return clues.clues();
		}

		for (final ArchiveFile file : files) {
			for (final FileContentClueFinder finder : fileContentClueFinders) {
				if (!finder.matches(file.name())) {
					continue;
				}
				progressor.throwIfCancelled();
				from(finder, session, file).ifPresent(clues::addAll);
			}
		}

		final Path normalizedFolder = folder.path().normalize();
		final List<Path> relativeFiles = files.stream()
				.map(file -> normalizedFolder.relativize(file.path().normalize())).toList();
		for (final FileNameClueFinder finder : fileNameClueFinders) {
			clues.addAll(finder.find(relativeFiles));
		}
		return clues.clues();
	}

	/**
	 * Enriches clues already found locally with observations from the
	 * configured post-order tree finders.
	 * <p>
	 * The local clues arrive checked and are not inspected again; only what the
	 * tree finders add is examined, and it is examined against them.
	 */
	public Clues enrich(final Clues localClues, final ArchiveFolderView folder, final Progressor progressor) {
		Objects.requireNonNull(localClues, "localClues");
		Objects.requireNonNull(folder, "folder");
		if (treeClueFinders.isEmpty()) {
			return localClues;
		}
		final ClueAccumulator clues = Clues.accumulator(localClues);
		for (final TreeClueFinder finder : treeClueFinders) {
			progressor.throwIfCancelled();
			clues.addAll(finder.find(folder));
		}
		return clues.clues();
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
