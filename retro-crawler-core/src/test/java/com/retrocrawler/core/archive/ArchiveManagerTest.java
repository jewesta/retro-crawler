package com.retrocrawler.core.archive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.retrocrawler.core.archive.clues.Archive;
import com.retrocrawler.core.archive.clues.ArchiveNode;
import com.retrocrawler.core.archive.clues.ArchivePathClueFinder;
import com.retrocrawler.core.archive.clues.Bucket;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.InternalClueKeys;
import com.retrocrawler.core.progress.ProgressCancelledException;
import com.retrocrawler.core.progress.Progressor;

class ArchiveManagerTest {

	@TempDir
	private Path temporaryDirectory;

	private final Progressor progressor = new Progressor();

	@Test
	void retrievesStoredArchiveWithoutCrawlingFilesystem() throws IOException {
		final ArchiveDescriptor descriptor = descriptor(temporaryDirectory.resolve("does-not-exist"));
		final Archive stored = emptyStoredArchive(descriptor);
		final RecordingRepository repository = new RecordingRepository(Optional.of(stored));
		final ArchiveManager manager = manager(descriptor, repository);

		final Archive result = manager.getArchive(progressor, ReindexScope.none());

		assertSame(stored, result);
		assertEquals(1, repository.retrieveCount);
		assertEquals(0, repository.stowawayCount);
	}

	@Test
	void rebindsStoredBucketsToTheCurrentlyConfiguredRoots() throws IOException {
		final Path configuredRoot = temporaryDirectory.resolve("desktop-mount");
		final ArchiveDescriptor descriptor = descriptor(configuredRoot);
		final ArchiveNode storedRoot = new ArchiveNode(".", null, null);
		final Archive stored = Archive.of(descriptor.getId(),
				List.of(Bucket.of(Path.of("/nas-container/archive"), storedRoot)));
		final RecordingRepository repository = new RecordingRepository(Optional.of(stored));

		final Archive rebound = manager(descriptor, repository).getArchive(progressor, ReindexScope.none());

		assertNotSame(stored, rebound);
		assertSame(storedRoot, rebound.getBuckets().getFirst().getRoot());
		assertEquals(configuredRoot.toString(), rebound.getBuckets().getFirst().getBasePath());
		assertEquals(0, repository.stowawayCount);
	}

	@Test
	void crawlsAndStowsAwayArchiveAfterRepositoryMiss() throws IOException {
		final Path archiveDirectory = Files.createDirectory(temporaryDirectory.resolve("archive"));
		final ArchiveDescriptor descriptor = descriptor(archiveDirectory);
		final RecordingRepository repository = new RecordingRepository(Optional.empty());
		final ArchiveManager manager = manager(descriptor, repository);

		final Archive result = manager.getArchive(progressor, ReindexScope.none());

		assertEquals(1, repository.retrieveCount);
		assertEquals(1, repository.stowawayCount);
		assertSame(result, repository.stowedAway);
	}

	@Test
	void reindexingBypassesRepositoryRetrievalAndReplacesArchive() throws IOException {
		final Path archiveDirectory = Files.createDirectory(temporaryDirectory.resolve("archive"));
		final ArchiveDescriptor descriptor = descriptor(archiveDirectory);
		final Archive stored = emptyStoredArchive(descriptor);
		final RecordingRepository repository = new RecordingRepository(Optional.of(stored));
		final ArchiveManager manager = manager(descriptor, repository);

		final Archive result = manager.getArchive(progressor, ReindexScope.all());

		assertNotSame(stored, result);
		assertEquals(0, repository.retrieveCount);
		assertEquals(1, repository.stowawayCount);
		assertSame(result, repository.stowedAway);
	}

	@Test
	void retrievalFailureCausesFilesystemToBeCrawledAgain() throws IOException {
		final Path archiveDirectory = Files.createDirectory(temporaryDirectory.resolve("archive"));
		final ArchiveDescriptor descriptor = descriptor(archiveDirectory);
		final RecordingRepository repository = new RecordingRepository(new RepositoryException("Unavailable"));
		final ArchiveManager manager = manager(descriptor, repository);

		final Archive result = manager.getArchive(progressor, ReindexScope.none());

		assertEquals(1, repository.retrieveCount);
		assertEquals(1, repository.stowawayCount);
		assertSame(result, repository.stowedAway);
	}

	@Test
	void stowawayFailureRemainsVisibleToCaller() throws IOException {
		final Path archiveDirectory = Files.createDirectory(temporaryDirectory.resolve("archive"));
		final ArchiveDescriptor descriptor = descriptor(archiveDirectory);
		final RecordingRepository repository = new RecordingRepository(Optional.empty());
		repository.stowawayFailure = new RepositoryException("Read only");
		final ArchiveManager manager = manager(descriptor, repository);

		assertThrows(RepositoryException.class, () -> manager.getArchive(progressor, ReindexScope.none()));
		assertEquals(1, repository.retrieveCount);
		assertEquals(1, repository.stowawayCount);
	}

	@Test
	void retainsRetrievedArchiveInMemory() throws IOException {
		final ArchiveDescriptor descriptor = descriptor(temporaryDirectory.resolve("does-not-exist"));
		final Archive stored = emptyStoredArchive(descriptor);
		final RecordingRepository repository = new RecordingRepository(Optional.of(stored));
		final ArchiveManager manager = manager(descriptor, repository);

		assertSame(stored, manager.getArchive(progressor, ReindexScope.none()));
		assertSame(stored, manager.getArchive(progressor, ReindexScope.none()));
		assertEquals(1, repository.retrieveCount);
	}

	@Test
	void cancellationDoesNotStowAwayAPartialArchive() throws IOException {
		final Path archiveDirectory = Files.createDirectory(temporaryDirectory.resolve("archive"));
		final ArchiveDescriptor descriptor = descriptor(archiveDirectory);
		final RecordingRepository repository = new RecordingRepository(Optional.empty());
		final Progressor cancellingProgressor = new Progressor();
		final ArchivePathClueFinder clueFinder = new ArchivePathClueFinder(folder -> {
			cancellingProgressor.cancel("Stop.");
			return Set.of(Clue.of("folder", folder));
		}, List.of(), List.of());
		final ArchiveDigger digger = new ArchiveDigger(descriptor, clueFinder,
				new CrawlPlanning(1, 0, 1, java.time.Duration.ofSeconds(1)));
		final ArchiveManager manager = new ArchiveManager(descriptor, digger, repository);

		assertThrows(ProgressCancelledException.class,
				() -> manager.getArchive(cancellingProgressor, ReindexScope.all()));
		assertEquals(0, repository.stowawayCount);
	}

	@Test
	void reindexesOneSubtreeAndRetainsTheRestOfTheStoredArchive() throws IOException {
		final Path archiveDirectory = Files.createDirectory(temporaryDirectory.resolve("archive"));
		final Path selected = Files.createDirectory(archiveDirectory.resolve("selected"));
		final Path oldFolder = Files.createDirectory(selected.resolve("old"));
		final Path untouched = Files.createDirectory(archiveDirectory.resolve("untouched"));
		Files.createDirectory(untouched.resolve("original"));
		final ArchiveDescriptor descriptor = descriptor(archiveDirectory);
		final RecordingRepository repository = new RecordingRepository(Optional.empty());
		final ArchiveManager manager = manager(descriptor, repository);

		final Archive original = manager.getArchive(new Progressor(), ReindexScope.all());
		final ArchiveNode originalSelected = node(original, "selected");
		final ArchiveNode originalUntouched = node(original, "untouched");
		final String originalSelectedId = technicalId(originalSelected);

		Files.move(oldFolder, selected.resolve("renamed"));
		Files.createDirectory(untouched.resolve("created-after-index"));

		final Archive refreshed = manager.getArchive(new Progressor(), ReindexScope.subtree(selected));

		assertEquals(List.of("renamed"), childFolders(node(refreshed, "selected")));
		assertEquals(List.of("original"), childFolders(node(refreshed, "untouched")));
		assertSame(originalUntouched, node(refreshed, "untouched"));
		assertNotSame(originalSelected, node(refreshed, "selected"));
		assertEquals(originalSelectedId, technicalId(node(refreshed, "selected")));
		assertEquals(2, repository.stowawayCount);
		assertSame(refreshed, repository.stowedAway);
	}

	@Test
	void retrievesAndPartiallyReindexesAJsonArchiveInANewCrawlerSession() throws IOException {
		final Path archiveDirectory = Files.createDirectory(temporaryDirectory.resolve("archive"));
		final Path selected = Files.createDirectory(archiveDirectory.resolve("selected"));
		final Path oldFolder = Files.createDirectory(selected.resolve("old"));
		final ArchiveDescriptor descriptor = descriptor(archiveDirectory);
		final Repository repository = new JsonFileRepository(temporaryDirectory.resolve("repository"));
		final Archive original = manager(descriptor, repository).getArchive(new Progressor(), ReindexScope.all());
		final String originalSelectedId = technicalId(node(original, "selected"));
		Files.move(oldFolder, selected.resolve("renamed"));

		final Archive refreshed = manager(descriptor, repository).getArchive(new Progressor(),
				ReindexScope.subtree(selected));

		assertEquals(List.of("renamed"), childFolders(node(refreshed, "selected")));
		assertEquals(originalSelectedId, technicalId(node(refreshed, "selected")));
	}

	@Test
	void reindexesMultipleSubtreesAndEliminatesNestedSelections() throws IOException {
		final Path archiveDirectory = Files.createDirectory(temporaryDirectory.resolve("archive"));
		final Path first = Files.createDirectory(archiveDirectory.resolve("first"));
		final Path firstOld = Files.createDirectory(first.resolve("old"));
		final Path second = Files.createDirectory(archiveDirectory.resolve("second"));
		final Path secondOld = Files.createDirectory(second.resolve("old"));
		final ArchiveDescriptor descriptor = descriptor(archiveDirectory);
		final RecordingRepository repository = new RecordingRepository(Optional.empty());
		final ArchiveManager manager = manager(descriptor, repository);
		manager.getArchive(new Progressor(), ReindexScope.all());
		final Path firstRenamed = Files.move(firstOld, first.resolve("renamed"));
		Files.move(secondOld, second.resolve("renamed"));

		final Archive refreshed = manager.getArchive(new Progressor(),
				ReindexScope.subtrees(first, firstRenamed, second));

		assertEquals(List.of("renamed"), childFolders(node(refreshed, "first")));
		assertEquals(List.of("renamed"), childFolders(node(refreshed, "second")));
		assertEquals(2, repository.stowawayCount);
	}

	@Test
	void partialReindexRequiresACompleteStoredArchive() throws IOException {
		final Path archiveDirectory = Files.createDirectory(temporaryDirectory.resolve("archive"));
		final Path selected = Files.createDirectory(archiveDirectory.resolve("selected"));
		final ArchiveDescriptor descriptor = descriptor(archiveDirectory);
		final RecordingRepository repository = new RecordingRepository(Optional.empty());
		final ArchiveManager manager = manager(descriptor, repository);

		final IllegalStateException failure = assertThrows(IllegalStateException.class,
				() -> manager.getArchive(new Progressor(), ReindexScope.subtree(selected)));

		assertTrue(failure.getMessage().contains("complete archive"));
		assertEquals(1, repository.retrieveCount);
		assertEquals(0, repository.stowawayCount);
	}

	@Test
	void renamedScopeMustBeReindexedThroughItsStoredParent() throws IOException {
		final Path archiveDirectory = Files.createDirectory(temporaryDirectory.resolve("archive"));
		final Path selected = Files.createDirectory(archiveDirectory.resolve("selected"));
		final ArchiveDescriptor descriptor = descriptor(archiveDirectory);
		final RecordingRepository repository = new RecordingRepository(Optional.empty());
		final ArchiveManager manager = manager(descriptor, repository);
		manager.getArchive(new Progressor(), ReindexScope.all());
		final Path renamed = Files.move(selected, archiveDirectory.resolve("renamed"));

		final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
				() -> manager.getArchive(new Progressor(), ReindexScope.subtree(renamed)));

		assertTrue(failure.getMessage().contains("re-index its parent"));
		assertEquals(1, repository.stowawayCount);
	}

	@Test
	void failedPartialStowawayRetainsThePreviousInMemoryArchive() throws IOException {
		final Path archiveDirectory = Files.createDirectory(temporaryDirectory.resolve("archive"));
		final Path selected = Files.createDirectory(archiveDirectory.resolve("selected"));
		final ArchiveDescriptor descriptor = descriptor(archiveDirectory);
		final RecordingRepository repository = new RecordingRepository(Optional.empty());
		final ArchiveManager manager = manager(descriptor, repository);
		final Archive original = manager.getArchive(new Progressor(), ReindexScope.all());
		Files.createDirectory(selected.resolve("new"));
		repository.stowawayFailure = new RepositoryException("Read only");

		assertThrows(RepositoryException.class,
				() -> manager.getArchive(new Progressor(), ReindexScope.subtree(selected)));

		assertSame(original, manager.getArchive(new Progressor(), ReindexScope.none()));
		assertEquals(2, repository.stowawayCount);
	}

	private static ArchiveNode node(final Archive archive, final String... folders) {
		ArchiveNode result = archive.getBuckets().getFirst().getRoot();
		for (final String folder : folders) {
			final ArchiveNode parent = result;
			result = Optional.ofNullable(parent.getChildren()).orElse(List.of()).stream()
					.filter(child -> folder.equals(child.getFolder())).findFirst().orElseThrow();
		}
		return result;
	}

	private static List<String> childFolders(final ArchiveNode node) {
		return Optional.ofNullable(node.getChildren()).orElse(List.of()).stream().map(ArchiveNode::getFolder).toList();
	}

	private static String technicalId(final ArchiveNode node) {
		return node.getArtifact().getClues().stream().filter(clue -> InternalClueKeys.ID.equals(clue.getKey()))
				.findFirst().orElseThrow().getValue().iterator().next();
	}

	private ArchiveDescriptor descriptor(final Path archiveDirectory) {
		return new ArchiveDescriptor(ArchiveId.of("test_archive"), "Test archive", List.of(archiveDirectory));
	}

	private static Archive emptyStoredArchive(final ArchiveDescriptor descriptor) {
		final Path root = descriptor.getPaths().iterator().next();
		return Archive.of(descriptor.getId(), List.of(Bucket.of(root, new ArchiveNode(".", null, null))));
	}

	private ArchiveManager manager(final ArchiveDescriptor descriptor, final Repository repository) {
		final ArchivePathClueFinder clueFinder = new ArchivePathClueFinder(
				folder -> Set.of(Clue.of("folder", folder)), List.of(), List.of());
		final ArchiveDigger digger = new ArchiveDigger(descriptor, clueFinder);
		return new ArchiveManager(descriptor, digger, repository);
	}

	private static final class RecordingRepository implements Repository {

		private final Optional<Archive> retrieved;

		private final RepositoryException retrievalFailure;

		private int retrieveCount;

		private int stowawayCount;

		private Archive stowedAway;

		private RepositoryException stowawayFailure;

		private RecordingRepository(final Optional<Archive> retrieved) {
			this.retrieved = retrieved;
			this.retrievalFailure = null;
		}

		private RecordingRepository(final RepositoryException retrievalFailure) {
			this.retrieved = Optional.empty();
			this.retrievalFailure = retrievalFailure;
		}

		@Override
		public void stowaway(final Archive archive) {
			stowawayCount++;
			if (stowawayFailure != null) {
				throw stowawayFailure;
			}
			stowedAway = archive;
		}

		@Override
		public Optional<Archive> retrieve(final ArchiveId id) {
			retrieveCount++;
			if (retrievalFailure != null) {
				throw retrievalFailure;
			}
			return retrieved;
		}
	}

}
