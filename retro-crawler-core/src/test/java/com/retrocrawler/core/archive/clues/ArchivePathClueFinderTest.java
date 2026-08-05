package com.retrocrawler.core.archive.clues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import com.retrocrawler.core.progress.FailureMode;
import com.retrocrawler.core.progress.Progressor;

class ArchivePathClueFinderTest {

	@Test
	void passesFileNamesRelativeToTheCurrentFolder() {
		final Path rootPath = Path.of("/archive");
		final ArchiveFolder root = () -> rootPath;
		final ArchiveFolder folder = () -> rootPath.resolve("shelf/gear");
		final ArchiveFile image = () -> folder.path().resolve("front.jpeg");
		final FileNameClueFinder fileNames = paths -> Clues
				.of(Clue.of("image", paths.stream().map(FileNameClueFinder::portablePath).collect(Collectors.toSet())));
		final ArchivePathClueFinder finder = new ArchivePathClueFinder(null, List.of(), List.of(fileNames));

		final Clues clues = finder.find(folder, List.of(image), emptySession(root), new Progressor());

		assertEquals(Set.of("front.jpeg"), clues.iterator().next().value());
	}

	@Test
	void rejectsATreeClueClaimingAKeyAlreadyFoundLocally() {
		final ArchiveFolder root = () -> Path.of("/archive");
		final ArchiveFolder folder = () -> root.path().resolve("folder");
		final FolderNameClueFinder folderFinder = ignored -> Clues.of(Clue.of("bus", "ISA"));
		final TreeClueFinder treeFinder = ignored -> Clues.of(Clue.of("bus", "PCI"));
		final ArchivePathClueFinder finder = new ArchivePathClueFinder(folderFinder, List.of(), List.of(),
				List.of(treeFinder));
		final Progressor progressor = new Progressor();
		final Clues localClues = finder.find(folder, List.of(), emptySession(root), progressor);

		final ClueFindingException failure = assertThrows(ClueFindingException.class,
				() -> finder.enrich(localClues, emptyFolder(), progressor));

		assertEquals(ClueSourceKind.FOLDER_TREE, failure.source().orElseThrow().kind());
		assertInstanceOf(DuplicateClueException.class, failure.getCause());
		assertTrue(failure.getMessage().contains("bus"));
	}

	@Test
	void keepsAnonymousCluesFromDifferentFinders() {
		final ArchiveFolder root = () -> Path.of("/archive");
		final ArchiveFolder folder = () -> root.path().resolve("folder");
		final FolderNameClueFinder folderFinder = ignored -> Clues.of(Clue.of("folder observation"));
		final TreeClueFinder treeFinder = ignored -> Clues.of(Clue.of("tree observation"));
		final ArchivePathClueFinder finder = new ArchivePathClueFinder(folderFinder, List.of(), List.of(),
				List.of(treeFinder));
		final Progressor progressor = new Progressor();
		final Clues localClues = finder.find(folder, List.of(), emptySession(root), progressor);

		final Clues clues = finder.enrich(localClues, emptyFolder(), progressor);

		assertEquals(Set.of("folder observation", "tree observation"),
				clues.stream().flatMap(clue -> clue.value().stream()).collect(Collectors.toSet()));
		assertEquals(2, clues.stream().map(Clue::key).distinct().count());
	}

	@Test
	void leavesLocalCluesUntouchedWhenNoTreeFinderIsConfigured() {
		final ArchiveFolder root = () -> Path.of("/archive");
		final ArchiveFolder folder = () -> root.path().resolve("folder");
		final ArchivePathClueFinder finder = new ArchivePathClueFinder(ignored -> Clues.of(Clue.of("bus", "ISA")),
				List.of(), List.of());
		final Progressor progressor = new Progressor();
		final Clues localClues = finder.find(folder, List.of(), emptySession(root), progressor);

		assertSame(localClues, finder.enrich(localClues, emptyFolder(), progressor));
	}

	@Test
	void failLateRecordsAnArbitraryFinderExceptionAndContinuesWithIndependentFinders() {
		final Path rootPath = Path.of("/archive");
		final ArchiveFolder root = () -> rootPath;
		final ArchiveFolder folder = () -> rootPath.resolve("folder");
		final ArchiveFile file = () -> folder.path().resolve("front.jpeg");
		final IllegalStateException randomFailure = new IllegalStateException("Unexpected finder failure.");
		final FolderNameClueFinder broken = ignored -> {
			throw randomFailure;
		};
		final FileNameClueFinder working = ignored -> Clues.of(Clue.of("image", "front.jpeg"));
		final ArchivePathClueFinder finder = new ArchivePathClueFinder(broken, List.of(), List.of(working));
		final Progressor progressor = new Progressor(FailureMode.FAIL_LATE);

		final Clues clues = finder.find(folder, List.of(file), emptySession(root), progressor);

		assertEquals(Set.of("front.jpeg"), clues.get("image").orElseThrow().value());
		assertEquals(1, progressor.failureCount());
		final ClueFindingException recorded = assertInstanceOf(ClueFindingException.class,
				progressor.failures().getFirst());
		assertSame(randomFailure, recorded.getCause());
		assertEquals(ClueSourceKind.FOLDER_NAME, recorded.source().orElseThrow().kind());
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
