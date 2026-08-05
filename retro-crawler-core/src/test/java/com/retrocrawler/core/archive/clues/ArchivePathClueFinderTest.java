package com.retrocrawler.core.archive.clues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.source.ArchiveFile;
import com.retrocrawler.core.archive.source.ArchiveFileAccessor;
import com.retrocrawler.core.archive.source.ArchiveFolder;
import com.retrocrawler.core.archive.source.ArchiveListing;
import com.retrocrawler.core.archive.source.ArchiveSession;
import com.retrocrawler.core.progress.Progressor;

class ArchivePathClueFinderTest {

	@Test
	void passesFileNamesRelativeToTheCurrentFolder() {
		final Path rootPath = Path.of("/archive");
		final ArchiveFolder root = () -> rootPath;
		final ArchiveFolder folder = () -> rootPath.resolve("shelf/gear");
		final ArchiveFile image = () -> folder.path().resolve("front.jpeg");
		final FileNameClueFinder fileNames = paths -> Set
				.of(Clue.of("image", paths.stream().map(FileNameClueFinder::portablePath).collect(Collectors.toSet())));
		final ArchivePathClueFinder finder = new ArchivePathClueFinder(null, List.of(), List.of(fileNames));

		final Set<Clue> clues = finder.find(folder, List.of(image), emptySession(root), new Progressor());

		assertEquals(Set.of("front.jpeg"), clues.iterator().next().value());
	}

	@Test
	void rekeysCollidingAnonymousCluesInsteadOfMergingTheirValues() {
		final Clue first = new Clue("_deadbeef", Set.of("first"));
		final Clue second = new Clue("_deadbeef", Set.of("second"));
		final ArchivePathClueFinder finder = new ArchivePathClueFinder(ignored -> Set.of(), List.of(), List.of());

		final Set<Clue> merged = finder.enrich(Set.of(first, second), emptyFolder(), new Progressor());

		assertEquals(2, merged.size());
		assertTrue(merged.stream().allMatch(clue -> clue.value().size() == 1));
		assertEquals(Set.of("first", "second"),
				merged.stream().flatMap(clue -> clue.value().stream()).collect(Collectors.toSet()));
		final Set<String> keys = merged.stream().map(Clue::key).collect(Collectors.toSet());
		assertEquals(2, keys.size());
		assertTrue(keys.contains("_deadbeef"));
		assertTrue(keys.stream().allMatch(key -> key.matches("_[a-z0-9]{8}")));
	}

	@Test
	void rejectsMoreThanOneNamedClueForTheSameKey() {
		final Clue first = Clue.of("bus", "ISA");
		final Clue second = Clue.of("bus", "PCI");
		final ArchivePathClueFinder finder = new ArchivePathClueFinder(ignored -> Set.of(), List.of(), List.of());

		assertThrows(DuplicateClueException.class,
				() -> finder.enrich(Set.of(first, second), emptyFolder(), new Progressor()));
	}

	@Test
	void keepsAnonymousCluesFromDifferentFinders() {
		final ArchiveFolder root = () -> Path.of("/archive");
		final ArchiveFolder folder = () -> root.path().resolve("folder");
		final FolderNameClueFinder folderFinder = ignored -> Set.of(Clue.of("folder observation"));
		final TreeClueFinder treeFinder = ignored -> Set.of(Clue.of("tree observation"));
		final ArchivePathClueFinder finder = new ArchivePathClueFinder(folderFinder, List.of(), List.of(),
				List.of(treeFinder));
		final Progressor progressor = new Progressor();
		final Set<Clue> localClues = finder.find(folder, List.of(), emptySession(root), progressor);

		final Set<Clue> clues = finder.enrich(localClues, emptyFolder(), progressor);

		assertEquals(Set.of("folder observation", "tree observation"),
				clues.stream().flatMap(clue -> clue.value().stream()).collect(Collectors.toSet()));
		assertEquals(2, clues.stream().map(Clue::key).distinct().count());
	}

	private static ArchiveFolderView emptyFolder() {
		return new ArchiveFolderView() {

			@Override
			public String name() {
				return "folder";
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
	}

	private static ArchiveSession emptySession(final ArchiveFolder root) {
		return new ArchiveSession() {

			@Override
			public ArchiveFolder root() {
				return root;
			}

			@Override
			public ArchiveListing list(final ArchiveFolder folder) {
				return new ArchiveListing(List.of(), List.of());
			}

			@Override
			public <T> Optional<T> access(final ArchiveFile file, final ArchiveFileAccessor<T> accessor) {
				return Optional.empty();
			}

			@Override
			public void close() {
			}
		};
	}
}
