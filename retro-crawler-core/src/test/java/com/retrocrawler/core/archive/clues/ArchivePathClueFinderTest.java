package com.retrocrawler.core.archive.clues;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.retrocrawler.core.annotation.RetroArchive;
import com.retrocrawler.core.archive.ArchivePath;
import com.retrocrawler.core.progress.Progressor;

class ArchivePathClueFinderTest {

	private static final Progressor SILENT_PROGRESSOR = new Progressor();

	@TempDir
	private Path folder;

	@Test
	void collapsesCorroboratingCluesFromDifferentFinders() throws IOException {
		final Path file = Files.createFile(folder.resolve("evidence.txt"));
		final ArchivePathClueFinder finder = new ArchivePathClueFinder(
				name -> Set.of(Clue.of("bus", "AGP")),
				List.of(),
				List.of(files -> Set.of(Clue.of("bus", "AGP"))));

		final Set<Clue> clues = finder.find(new ArchivePath(folder, List.of(file)), SILENT_PROGRESSOR);

		assertEquals(Set.of("AGP"), clue(clues, "bus").getValue());
	}

	@Test
	void retainsConflictingValuesUnderTheirSemanticKey() throws IOException {
		final Path file = Files.createFile(folder.resolve("evidence.txt"));
		final ArchivePathClueFinder finder = new ArchivePathClueFinder(
				name -> Set.of(Clue.of("bus", "AGP")),
				List.of(),
				List.of(files -> Set.of(Clue.of("bus", "PCI"))));

		final Set<Clue> clues = finder.find(new ArchivePath(folder, List.of(file)), SILENT_PROGRESSOR);

		assertEquals(Set.of("AGP", "PCI"), clue(clues, "bus").getValue());
	}

	@Test
	void alsoMergesRepeatedKeysReturnedByOneFinder() {
		final ArchivePathClueFinder finder = new ArchivePathClueFinder(
				name -> Set.of(Clue.of("bus", "AGP"), Clue.of("bus", "PCI")),
				List.of(),
				List.of());

		final Set<Clue> clues = finder.find(new ArchivePath(folder, List.of()), SILENT_PROGRESSOR);

		assertEquals(Set.of("AGP", "PCI"), clue(clues, "bus").getValue());
	}

	@Test
	void createsTreeOnlyConfigurationFromArchiveAnnotation() {
		final RetroArchive.LookAt lookAt = TreeConfiguredArchive.class.getAnnotation(RetroArchive.class).findClues();
		final ArchivePathClueFinder finder = ArchivePathClueFinder.of(lookAt);
		final ArchiveFolderView view = new ArchiveFolderView() {

			@Override
			public String name() {
				return "gear";
			}

			@Override
			public List<ArchiveFolderView> folders() {
				return List.of();
			}

			@Override
			public List<ArchiveFileView> files() {
				return List.of();
			}
		};

		final Set<Clue> localClues = finder.find(new ArchivePath(folder, List.of()), SILENT_PROGRESSOR);
		final Set<Clue> clues = finder.enrich(localClues, view, SILENT_PROGRESSOR);

		assertEquals(Set.of("gear"), clue(clues, "tree").getValue());
	}

	@Test
	void mergesTreeCluesWithExistingLocalClues() {
		final ArchivePathClueFinder finder = new ArchivePathClueFinder(null, List.of(), List.of(),
				List.of(view -> Set.of(Clue.of("origin", "conversation"))));
		final ArchiveFolderView view = new ArchiveFolderView() {

			@Override
			public String name() {
				return "gear";
			}

			@Override
			public List<ArchiveFolderView> folders() {
				return List.of();
			}

			@Override
			public List<ArchiveFileView> files() {
				return List.of();
			}
		};

		final Set<Clue> clues = finder.enrich(Set.of(Clue.of("origin", "folder")), view, SILENT_PROGRESSOR);

		assertEquals(Set.of("folder", "conversation"), clue(clues, "origin").getValue());
	}

	@Test
	void acceptsFilesAlreadyClassifiedByTheDigger() throws IOException {
		final Path classifiedFile = Files.createFile(folder.resolve("classified.txt"));
		final ArchivePathClueFinder finder = new ArchivePathClueFinder(null, List.of(), List.of(files -> {
			final String names = files.stream().map(path -> path.getFileName().toString())
					.collect(java.util.stream.Collectors.joining(","));
			return Set.of(Clue.of("files", names));
		}));
		Files.delete(classifiedFile);

		final Set<Clue> clues = finder.find(new ArchivePath(folder, List.of()), List.of(classifiedFile),
				SILENT_PROGRESSOR);

		assertEquals(Set.of("classified.txt"), clue(clues, "files").getValue());
	}

	@Test
	void givesFileNameFindersArchiveRootRelativePaths() throws IOException {
		final Path gearFolder = Files.createDirectories(folder.resolve("shelf").resolve("gear"));
		final Path image = Files.createFile(gearFolder.resolve("front.jpeg"));
		final ArchivePathClueFinder finder = new ArchivePathClueFinder(null, List.of(),
				List.of(files -> Set.of(Clue.of("image", files.iterator().next().toString()))));

		final Set<Clue> clues = finder.find(new ArchivePath(folder, gearFolder, List.of(image)),
				SILENT_PROGRESSOR);

		assertEquals(Set.of(Path.of("shelf", "gear", "front.jpeg").toString()),
				clue(clues, "image").getValue());
	}

	private static Clue clue(final Set<Clue> clues, final String key) {
		return clues.stream().filter(candidate -> key.equals(candidate.getKey())).findFirst().orElseThrow();
	}

	@RetroArchive(id = "tree_configured", locations = "/not/read",
			findClues = @RetroArchive.LookAt(trees = ConfiguredTreeClueFinder.class))
	private static final class TreeConfiguredArchive {
	}

	public static final class ConfiguredTreeClueFinder implements TreeClueFinder {

		@Override
		public Set<Clue> find(final ArchiveFolderView folder) {
			return Set.of(Clue.of("tree", folder.name()));
		}
	}
}
