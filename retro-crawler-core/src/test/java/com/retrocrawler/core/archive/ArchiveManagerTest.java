package com.retrocrawler.core.archive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import com.retrocrawler.core.progress.ProgressCancelledException;
import com.retrocrawler.core.progress.Progressor;

class ArchiveManagerTest {

	@TempDir
	private Path temporaryDirectory;

	private final Progressor progressor = new Progressor();

	@Test
	void retrievesStoredArchiveWithoutCrawlingFilesystem() throws IOException {
		final ArchiveDescriptor descriptor = descriptor(temporaryDirectory.resolve("does-not-exist"));
		final Archive stored = Archive.of(descriptor.getId(), List.of());
		final RecordingRepository repository = new RecordingRepository(Optional.of(stored));
		final ArchiveManager manager = manager(descriptor, repository);

		final Archive result = manager.getArchive(progressor);

		assertSame(stored, result);
		assertEquals(1, repository.retrieveCount);
		assertEquals(0, repository.stowawayCount);
	}

	@Test
	void crawlsAndStowsAwayArchiveAfterRepositoryMiss() throws IOException {
		final Path archiveDirectory = Files.createDirectory(temporaryDirectory.resolve("archive"));
		final ArchiveDescriptor descriptor = descriptor(archiveDirectory);
		final RecordingRepository repository = new RecordingRepository(Optional.empty());
		final ArchiveManager manager = manager(descriptor, repository);

		final Archive result = manager.getArchive(progressor);

		assertEquals(1, repository.retrieveCount);
		assertEquals(1, repository.stowawayCount);
		assertSame(result, repository.stowedAway);
	}

	@Test
	void reindexingBypassesRepositoryRetrievalAndReplacesArchive() throws IOException {
		final Path archiveDirectory = Files.createDirectory(temporaryDirectory.resolve("archive"));
		final ArchiveDescriptor descriptor = descriptor(archiveDirectory);
		final Archive stored = Archive.of(descriptor.getId(), List.of());
		final RecordingRepository repository = new RecordingRepository(Optional.of(stored));
		final ArchiveManager manager = manager(descriptor, repository);

		final Archive result = manager.getArchive(progressor, true);

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

		final Archive result = manager.getArchive(progressor);

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

		assertThrows(RepositoryException.class, () -> manager.getArchive(progressor));
		assertEquals(1, repository.retrieveCount);
		assertEquals(1, repository.stowawayCount);
	}

	@Test
	void retainsRetrievedArchiveInMemory() throws IOException {
		final ArchiveDescriptor descriptor = descriptor(temporaryDirectory.resolve("does-not-exist"));
		final Archive stored = Archive.of(descriptor.getId(), List.of());
		final RecordingRepository repository = new RecordingRepository(Optional.of(stored));
		final ArchiveManager manager = manager(descriptor, repository);

		assertSame(stored, manager.getArchive(progressor));
		assertSame(stored, manager.getArchive(progressor));
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

		assertThrows(ProgressCancelledException.class, () -> manager.getArchive(cancellingProgressor, true));
		assertEquals(0, repository.stowawayCount);
	}

	private ArchiveDescriptor descriptor(final Path archiveDirectory) {
		return new ArchiveDescriptor(ArchiveId.of("test_archive"), "Test archive", List.of(archiveDirectory));
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
