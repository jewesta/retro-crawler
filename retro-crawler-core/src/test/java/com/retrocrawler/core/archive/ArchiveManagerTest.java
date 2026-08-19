package com.retrocrawler.core.archive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.retrocrawler.core.CrawlException;
import com.retrocrawler.core.FailureMode;
import com.retrocrawler.core.Journal;
import com.retrocrawler.core.archive.clues.Archive;
import com.retrocrawler.core.archive.clues.ArchiveNode;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.ClueFinder;
import com.retrocrawler.core.archive.clues.Clues;
import com.retrocrawler.core.archive.clues.InternalClueKeys;
import com.retrocrawler.core.progress.ProgressCancelledException;

class ArchiveManagerTest {

	@TempDir
	private Path temporaryDirectory;

	private final Journal journal = new Journal();

	@Test
	void retrievesStoredArchiveWithoutCrawlingFilesystem() throws IOException {
		final ArchiveDescriptor descriptor = descriptor(temporaryDirectory.resolve("does-not-exist"));
		final Archive stored = emptyStoredArchive(descriptor);
		final RecordingRepository repository = new RecordingRepository(Optional.of(stored));
		final ArchiveManager manager = manager(descriptor, repository);

		final Archive result = manager.archive(journal, ReindexScope.none());

		assertSame(stored, result);
		assertEquals(1, repository.retrieveCount);
		assertEquals(0, repository.stowawayCount);
	}

	@Test
	void rebindsAStoredArchiveToItsCurrentlyConfiguredRoot() throws IOException {
		final Path configuredRoot = temporaryDirectory.resolve("desktop-mount");
		final ArchiveDescriptor descriptor = descriptor(configuredRoot);
		final ArchiveNode storedRoot = new ArchiveNode(".", null, null);
		final Archive stored = Archive.of("test_collection", descriptor.id(), Path.of("/nas-container/archive"),
				storedRoot);
		final RecordingRepository repository = new RecordingRepository(Optional.of(stored));

		final Archive rebound = manager(descriptor, repository).archive(journal, ReindexScope.none());

		assertNotSame(stored, rebound);
		assertSame(storedRoot, rebound.root());
		assertEquals(configuredRoot.toString(), rebound.basePath());
		assertEquals(0, repository.stowawayCount);
	}

	@Test
	void crawlsAndStowsAwayArchiveAfterRepositoryMiss() throws IOException {
		final Path archiveDirectory = Files.createDirectory(temporaryDirectory.resolve("archive"));
		final ArchiveDescriptor descriptor = descriptor(archiveDirectory);
		final RecordingRepository repository = new RecordingRepository(Optional.empty());
		final ArchiveManager manager = manager(descriptor, repository);

		final Archive result = manager.archive(journal, ReindexScope.none());

		assertEquals(1, repository.retrieveCount);
		assertEquals(1, repository.stowawayCount);
		assertSame(result, repository.stowedAway);
		assertEquals("test_collection", result.collectionId());
	}

	@Test
	void reindexingBypassesRepositoryRetrievalAndReplacesArchive() throws IOException {
		final Path archiveDirectory = Files.createDirectory(temporaryDirectory.resolve("archive"));
		final ArchiveDescriptor descriptor = descriptor(archiveDirectory);
		final Archive stored = emptyStoredArchive(descriptor);
		final RecordingRepository repository = new RecordingRepository(Optional.of(stored));
		final ArchiveManager manager = manager(descriptor, repository);

		final Archive result = manager.archive(journal, ReindexScope.all());

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

		final Archive result = manager.archive(journal, ReindexScope.none());

		assertEquals(1, repository.retrieveCount);
		assertEquals(1, repository.stowawayCount);
		assertSame(result, repository.stowedAway);
	}

	@Test
	void archiveFromAnotherCollectionIsCrawledAgain() throws IOException {
		final Path archiveDirectory = Files.createDirectory(temporaryDirectory.resolve("archive"));
		final ArchiveDescriptor descriptor = descriptor(archiveDirectory);
		final Archive stored = Archive.of("another_collection", descriptor.id(), archiveDirectory,
				new ArchiveNode(".", null, null));
		final RecordingRepository repository = new RecordingRepository(Optional.of(stored));
		final ArchiveManager manager = manager(descriptor, repository);

		final Archive result = manager.archive(journal, ReindexScope.none());

		assertEquals("test_collection", result.collectionId());
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

		assertThrows(RepositoryException.class, () -> manager.archive(journal, ReindexScope.none()));
		assertEquals(1, repository.retrieveCount);
		assertEquals(1, repository.stowawayCount);
	}

	@Test
	void retainsRetrievedArchiveInMemory() throws IOException {
		final ArchiveDescriptor descriptor = descriptor(temporaryDirectory.resolve("does-not-exist"));
		final Archive stored = emptyStoredArchive(descriptor);
		final RecordingRepository repository = new RecordingRepository(Optional.of(stored));
		final ArchiveManager manager = manager(descriptor, repository);

		assertSame(stored, manager.archive(journal, ReindexScope.none()));
		assertSame(stored, manager.archive(journal, ReindexScope.none()));
		assertEquals(1, repository.retrieveCount);
	}

	@Test
	void cancellationDoesNotStowAwayAPartialArchive() throws IOException {
		final Path archiveDirectory = Files.createDirectory(temporaryDirectory.resolve("archive"));
		final ArchiveDescriptor descriptor = descriptor(archiveDirectory);
		final RecordingRepository repository = new RecordingRepository(Optional.empty());
		final Journal cancellingJournal = new Journal();
		final ClueFinder clueFinder = new TestClueFinder(folder -> {
			cancellingJournal.cancel("Stop.");
			return Clues.of(Clue.of("folder", folder.name()));
		});
		final ArchiveDigger digger = new ArchiveDigger(new TestArchiveDefinition(descriptor, clueFinder),
				new CrawlPlanning(1, 0, 1, java.time.Duration.ofSeconds(1)));
		final ArchiveManager manager = new ArchiveManager(descriptor, digger, repository);

		assertThrows(ProgressCancelledException.class, () -> manager.archive(cancellingJournal, ReindexScope.all()));
		assertEquals(0, repository.stowawayCount);
	}

	@Test
	void failLateDoesNotStowAwayAnArchiveContainingFailedFolders() throws IOException {
		final Path archiveDirectory = Files.createDirectory(temporaryDirectory.resolve("archive"));
		Files.createDirectory(archiveDirectory.resolve("broken"));
		final ArchiveDescriptor descriptor = descriptor(archiveDirectory);
		final RecordingRepository repository = new RecordingRepository(Optional.empty());
		final ClueFinder clueFinder = new TestClueFinder(folder -> {
			throw new IllegalStateException("Finder broke at " + folder);
		});
		final ArchiveDigger digger = new ArchiveDigger(new TestArchiveDefinition(descriptor, clueFinder));
		final ArchiveManager manager = new ArchiveManager(descriptor, digger, repository);
		final Journal failLate = new Journal(FailureMode.FAIL_LATE);

		final CrawlException failure = assertThrows(CrawlException.class,
				() -> manager.archive(failLate, ReindexScope.all()));

		assertEquals(2, failure.failures().size());
		assertEquals(failLate.failures(), failure.failures());
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

		final Archive original = manager.archive(new Journal(), ReindexScope.all());
		final ArchiveNode originalSelected = node(original, "selected");
		final ArchiveNode originalUntouched = node(original, "untouched");
		final String originalSelectedId = technicalId(originalSelected);

		Files.move(oldFolder, selected.resolve("renamed"));
		Files.createDirectory(untouched.resolve("created-after-index"));

		final Archive refreshed = manager.archive(new Journal(), ReindexScope.subtree(ari(descriptor, selected)));

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
		final Instant fullCrawl = Instant.parse("2026-08-05T08:00:00Z");
		final Instant partialCrawl = Instant.parse("2026-08-05T09:00:00Z");
		final Path archiveDirectory = Files.createDirectory(temporaryDirectory.resolve("archive"));
		final Path selected = Files.createDirectory(archiveDirectory.resolve("selected"));
		final Path oldFolder = Files.createDirectory(selected.resolve("old"));
		Files.createDirectory(archiveDirectory.resolve("untouched"));
		final ArchiveDescriptor descriptor = descriptor(archiveDirectory);
		final Repository repository = new JsonFileRepository(temporaryDirectory.resolve("repository"));
		final Archive original = manager(descriptor, repository, Clock.fixed(fullCrawl, ZoneOffset.UTC))
				.archive(new Journal(), ReindexScope.all());
		final String originalSelectedId = technicalId(node(original, "selected"));
		Files.move(oldFolder, selected.resolve("renamed"));

		final Archive refreshed = manager(descriptor, repository, Clock.fixed(partialCrawl, ZoneOffset.UTC))
				.archive(new Journal(), ReindexScope.subtree(ari(descriptor, selected)));

		assertEquals(List.of("renamed"), childFolders(node(refreshed, "selected")));
		assertEquals(originalSelectedId, technicalId(node(refreshed, "selected")));
		assertEquals(fullCrawl, refreshed.root().crawledAt());
		assertEquals(fullCrawl, node(refreshed, "untouched").crawledAt());
		assertEquals(partialCrawl, node(refreshed, "selected").crawledAt());
		assertEquals(partialCrawl, node(refreshed, "selected", "renamed").crawledAt());
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
		manager.archive(new Journal(), ReindexScope.all());
		final Path firstRenamed = Files.move(firstOld, first.resolve("renamed"));
		Files.move(secondOld, second.resolve("renamed"));

		final Archive refreshed = manager.archive(new Journal(),
				ReindexScope.subtrees(ari(descriptor, first), ari(descriptor, firstRenamed), ari(descriptor, second)));

		assertEquals(List.of("renamed"), childFolders(node(refreshed, "first")));
		assertEquals(List.of("renamed"), childFolders(node(refreshed, "second")));
		assertEquals(node(refreshed, "first").crawledAt(), node(refreshed, "second").crawledAt());
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
				() -> manager.archive(new Journal(), ReindexScope.subtree(ari(descriptor, selected))));

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
		manager.archive(new Journal(), ReindexScope.all());
		final Path renamed = Files.move(selected, archiveDirectory.resolve("renamed"));

		final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
				() -> manager.archive(new Journal(), ReindexScope.subtree(ari(descriptor, renamed))));

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
		final Archive original = manager.archive(new Journal(), ReindexScope.all());
		Files.createDirectory(selected.resolve("new"));
		repository.stowawayFailure = new RepositoryException("Read only");

		assertThrows(RepositoryException.class,
				() -> manager.archive(new Journal(), ReindexScope.subtree(ari(descriptor, selected))));

		assertSame(original, manager.archive(new Journal(), ReindexScope.none()));
		assertEquals(2, repository.stowawayCount);
	}

	private static ArchiveNode node(final Archive archive, final String... folders) {
		ArchiveNode result = archive.root();
		for (final String folder : folders) {
			final ArchiveNode parent = result;
			result = Optional.ofNullable(parent.children()).orElse(List.of()).stream()
					.filter(child -> folder.equals(child.folder())).findFirst().orElseThrow();
		}
		return result;
	}

	private static List<String> childFolders(final ArchiveNode node) {
		return Optional.ofNullable(node.children()).orElse(List.of()).stream().map(ArchiveNode::folder).toList();
	}

	private static String technicalId(final ArchiveNode node) {
		return node.artifact().clues().stream().filter(clue -> InternalClueKeys.ID.equals(clue.key())).findFirst()
				.orElseThrow().value().iterator().next();
	}

	private ArchiveDescriptor descriptor(final Path archiveDirectory) {
		return new ArchiveDescriptor(ArchiveId.of("test_archive"), "Test archive", archiveDirectory);
	}

	private static ARI ari(final ArchiveDescriptor descriptor, final Path sourcePath) {
		return ARI.of("test_collection", descriptor.id(),
				descriptor.root().normalize().relativize(sourcePath.normalize()));
	}

	private static Archive emptyStoredArchive(final ArchiveDescriptor descriptor) {
		return Archive.of("test_collection", descriptor.id(), descriptor.root(), new ArchiveNode(".", null, null));
	}

	private ArchiveManager manager(final ArchiveDescriptor descriptor, final Repository repository) {
		return manager(descriptor, repository, Clock.systemUTC());
	}

	private ArchiveManager manager(final ArchiveDescriptor descriptor, final Repository repository, final Clock clock) {
		final ClueFinder clueFinder = new TestClueFinder(folder -> Clues.of(Clue.of("folder", folder.name())));
		final ArchiveDigger digger = new ArchiveDigger(new TestArchiveDefinition(descriptor, clueFinder));
		return new ArchiveManager(descriptor, digger, repository, clock);
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
