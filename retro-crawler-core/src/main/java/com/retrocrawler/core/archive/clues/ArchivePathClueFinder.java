package com.retrocrawler.core.archive.clues;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.retrocrawler.core.annotation.RetroClues;
import com.retrocrawler.core.archive.source.ArchiveFile;
import com.retrocrawler.core.archive.source.ArchiveFolder;
import com.retrocrawler.core.archive.source.ArchiveSession;
import com.retrocrawler.core.progress.ProgressCancelledException;
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
			return session.access(file, finder::find);
		} catch (final IOException e) {
			throw new ClueFileIOException("Could not inspect clue file at: " + file.path(), e);
		}
	}

	/**
	 * Runs one finder while naming what it is reading, so that anything it
	 * throws is reported against that source instead of escaping bare. A finder
	 * that reported a position of its own keeps it.
	 * <p>
	 * The accumulation is inside the guard on purpose: a rejected duplicate
	 * surfaces when the clue is added, not when the finder returns.
	 */
	private static <T> T observing(final ClueAccumulator clues, final ClueSource source, final Progressor progressor,
			final Consumer<ClueFindingException> failures, final Supplier<T> observation, final T fallback) {
		progressor.throwIfCancelled();
		clues.observing(source);
		try {
			return observation.get();
		} catch (final ProgressCancelledException cancelled) {
			// Cancellation is the caller's decision, not a finding failure.
			throw cancelled;
		} catch (final RuntimeException failure) {
			failures.accept(ClueFindingException.from(source, failure));
			return fallback;
		}
	}

	/**
	 * Runs local clue finders against entries supplied by an archive source.
	 */
	public Clues find(final ArchiveFolder folder, final List<ArchiveFile> files, final ArchiveSession session,
			final Progressor progressor) {
		return find(folder, files, session, progressor, progressor::record);
	}

	/**
	 * Runs local clue finders and hands each independently contextualized
	 * failure to the supplied recorder. A recorder may attach the archive
	 * location before delegating to the progressor.
	 */
	public Clues find(final ArchiveFolder folder, final List<ArchiveFile> files, final ArchiveSession session,
			final Progressor progressor, final Consumer<ClueFindingException> failures) {
		Objects.requireNonNull(folder, "folder");
		Objects.requireNonNull(files, "files");
		Objects.requireNonNull(session, "session");
		Objects.requireNonNull(progressor, "progressor");
		Objects.requireNonNull(failures, "failures");

		/*
		 * One accumulator for every finder at this location. Each observation
		 * is checked once, as it arrives, against everything observed so far.
		 */
		final ClueAccumulator clues = Clues.accumulator();
		if (folderNameClueFinder != null) {
			observing(clues, ClueSource.folderName(folder.name(), folderNameClueFinder), progressor, failures, () -> {
				clues.addAll(folderNameClueFinder.find(folder.name()));
				return null;
			}, null);
		}
		if (files.isEmpty()) {
			return clues.clues();
		}

		for (final ArchiveFile file : files) {
			for (final FileContentClueFinder finder : fileContentClueFinders) {
				final ClueSource source = ClueSource.fileContent(file.name(), finder);
				if (!observing(clues, source, progressor, failures, () -> finder.matches(file.name()), false)) {
					continue;
				}
				progressor.throwIfCancelled();
				observing(clues, source, progressor, failures, () -> {
					from(finder, session, file).ifPresent(clues::addAll);
					return null;
				}, null);
			}
		}

		final Path normalizedFolder = folder.path().normalize();
		final List<Path> relativeFiles = files.stream()
				.map(file -> normalizedFolder.relativize(file.path().normalize())).toList();
		for (final FileNameClueFinder finder : fileNameClueFinders) {
			observing(clues, ClueSource.fileNames(finder), progressor, failures, () -> {
				clues.addAll(finder.find(relativeFiles));
				return null;
			}, null);
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
		return enrich(localClues, folder, progressor, progressor::record);
	}

	/**
	 * Runs tree finders while handing every failure to the supplied recorder.
	 */
	public Clues enrich(final Clues localClues, final ArchiveFolderView folder, final Progressor progressor,
			final Consumer<ClueFindingException> failures) {
		Objects.requireNonNull(localClues, "localClues");
		Objects.requireNonNull(folder, "folder");
		Objects.requireNonNull(progressor, "progressor");
		Objects.requireNonNull(failures, "failures");
		if (treeClueFinders.isEmpty()) {
			return localClues;
		}
		final ClueAccumulator clues = Clues.accumulator(localClues);
		for (final TreeClueFinder finder : treeClueFinders) {
			progressor.throwIfCancelled();
			observing(clues, ClueSource.folderTree(finder), progressor, failures, () -> {
				clues.addAll(finder.find(folder));
				return null;
			}, null);
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
