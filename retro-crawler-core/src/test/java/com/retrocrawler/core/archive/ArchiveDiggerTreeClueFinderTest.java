package com.retrocrawler.core.archive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.retrocrawler.core.archive.clues.ArchiveFileView;
import com.retrocrawler.core.archive.clues.ArchiveFolderView;
import com.retrocrawler.core.archive.clues.ArchiveNode;
import com.retrocrawler.core.archive.clues.ArchivePathClueFinder;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.ClueAccumulator;
import com.retrocrawler.core.archive.clues.ClueFindingException;
import com.retrocrawler.core.archive.clues.Clues;
import com.retrocrawler.core.archive.clues.TreeClueFinder;
import com.retrocrawler.core.archive.source.ArchiveSession;
import com.retrocrawler.core.progress.FailureMode;
import com.retrocrawler.core.progress.Progressor;

class ArchiveDiggerTreeClueFinderTest {

	@TempDir
	private Path root;

	@Test
	void findsParentCluesPostOrderThroughMetadataFoldersWithoutCrossingArtifactBoundaries() throws IOException {
		final Path metadata = Files.createDirectory(root.resolve("Kleinanzeigen"));
		Files.writeString(metadata.resolve("Konversation.txt"), "parent-origin");
		final Path childArtifact = Files.createDirectory(root.resolve("Child artifact"));
		final Path childMetadata = Files.createDirectory(childArtifact.resolve("Kleinanzeigen"));
		Files.writeString(childMetadata.resolve("Konversation.txt"), "child-origin");

		final List<String> inspectionOrder = new ArrayList<>();
		final List<String> rootFolders = new ArrayList<>();
		final TreeClueFinder treeFinder = folder -> {
			inspectionOrder.add(folder.name());
			if (folder.name().equals(root.getFileName().toString())) {
				folder.folders().stream().map(ArchiveFolderView::name).forEach(rootFolders::add);
			}
			return originClues(folder);
		};
		final ArchivePathClueFinder clueFinder = new ArchivePathClueFinder(
				name -> "Child artifact".equals(name) ? Clues.of(Clue.of("kind", "part")) : Clues.none(), List.of(),
				List.of(), List.of(treeFinder));
		final ArchiveDigger digger = new ArchiveDigger(new TestArchiveDefinition(descriptor(), clueFinder));

		final ArchiveNode archive = digger.dig(root, new Progressor());

		assertEquals(root.getFileName().toString(), inspectionOrder.getLast());
		assertEquals(List.of("Kleinanzeigen"), rootFolders);
		assertNotNull(archive.artifact());
		assertEquals(Set.of("parent-origin"), clue(archive, "origin-detail").value());

		final ArchiveNode child = child(archive, "Child artifact");
		assertNotNull(child.artifact());
		assertEquals(Set.of("child-origin"), clue(child, "origin-detail").value());
		assertNull(child(child, "Kleinanzeigen").artifact());
		assertFalse(rootFolders.contains("Child artifact"));
	}

	@Test
	void treeFinderAloneCanEstablishAnArtifact() throws IOException {
		Files.createDirectory(root.resolve("Kleinanzeigen"));
		final TreeClueFinder treeFinder = folder -> folder.folders().stream()
				.anyMatch(child -> "Kleinanzeigen".equals(child.name())) ? Clues.of(Clue.of("origin", "Kleinanzeigen"))
						: Clues.none();
		final ArchivePathClueFinder clueFinder = new ArchivePathClueFinder(null, List.of(), List.of(),
				List.of(treeFinder));

		final ArchiveNode archive = new ArchiveDigger(new TestArchiveDefinition(descriptor(), clueFinder)).dig(root,
				new Progressor());

		assertNotNull(archive.artifact());
		assertEquals(Set.of("Kleinanzeigen"), clue(archive, "origin").value());
		assertNull(child(archive, "Kleinanzeigen").artifact());
	}

	@Test
	void failLateCrawlsChildrenButKeepsAFailedFolderOpaqueToItsParent() throws IOException {
		final Path broken = Files.createDirectory(root.resolve("Broken artifact"));
		Files.createDirectory(broken.resolve("Nested artifact"));
		final IllegalStateException randomFailure = new IllegalStateException("Broken folder clue.");
		final List<String> rootFolders = new ArrayList<>();
		final ArchivePathClueFinder clueFinder = new ArchivePathClueFinder(name -> {
			if ("Broken artifact".equals(name)) {
				throw randomFailure;
			}
			return "Nested artifact".equals(name) ? Clues.of(Clue.of("kind", "part")) : Clues.none();
		}, List.of(), List.of(), List.of(folder -> {
			if (folder.name().equals(root.getFileName().toString())) {
				folder.folders().stream().map(ArchiveFolderView::name).forEach(rootFolders::add);
			}
			return Clues.none();
		}));
		final Progressor progressor = new Progressor(FailureMode.FAIL_LATE);

		final ArchiveDigger digger = new ArchiveDigger(new TestArchiveDefinition(descriptor(), clueFinder));
		final ArchiveNode archive;
		try (ArchiveSession session = digger.open(root)) {
			final ArchiveDigTarget target = digger.rootTarget(session);
			archive = digger.dig(target, digger.plan(List.of(target), progressor), progressor);
		}

		assertTrue(rootFolders.isEmpty());
		final ArchiveNode failed = child(archive, "Broken artifact");
		assertNull(failed.artifact());
		assertNotNull(child(failed, "Nested artifact").artifact());
		assertEquals(1, progressor.failureCount());
		final ClueFindingException recorded = (ClueFindingException) progressor.failures().getFirst();
		assertEquals(descriptor().id(), recorded.archiveId().orElseThrow());
		assertEquals(Path.of("Broken artifact"), recorded.folder().orElseThrow());
		assertSame(randomFailure, recorded.getCause());
	}

	private Clues originClues(final ArchiveFolderView folder) {
		final ClueAccumulator clues = Clues.accumulator();
		for (final ArchiveFolderView child : folder.folders()) {
			if (!"Kleinanzeigen".equals(child.name())) {
				continue;
			}
			for (final ArchiveFileView file : child.files()) {
				if ("Konversation.txt".equals(file.name())) {
					file.peek(this::read).ifPresent(value -> clues.add(Clue.of("origin-detail", value)));
				}
			}
		}
		return clues.clues();
	}

	private String read(final InputStream in) {
		try {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private ArchiveDescriptor descriptor() {
		return new ArchiveDescriptor(ArchiveId.of("tree_clue_finder_test"), "Tree clue finder test", root);
	}

	private static ArchiveNode child(final ArchiveNode parent, final String folder) {
		return parent.children().stream().filter(candidate -> folder.equals(candidate.folder())).findFirst()
				.orElseThrow();
	}

	private static Clue clue(final ArchiveNode node, final String key) {
		return node.artifact().clues().stream().filter(candidate -> key.equals(candidate.key())).findFirst()
				.orElseThrow();
	}
}
