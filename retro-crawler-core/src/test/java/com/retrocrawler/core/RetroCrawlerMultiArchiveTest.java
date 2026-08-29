package com.retrocrawler.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.retrocrawler.core.annotation.RetroClues;
import com.retrocrawler.core.annotation.RetroCollection;
import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.archive.InMemoryRepository;
import com.retrocrawler.core.archive.ReindexScope;
import com.retrocrawler.core.archive.clues.ArchiveFolderView;
import com.retrocrawler.core.archive.clues.ClueFinder;
import com.retrocrawler.core.archive.clues.ClueFindingException;
import com.retrocrawler.core.archive.clues.Clues;
import com.retrocrawler.core.archive.source.ArchiveFile;
import com.retrocrawler.core.archive.source.ArchiveFileAccessor;
import com.retrocrawler.core.archive.source.ArchiveFolder;
import com.retrocrawler.core.archive.source.ArchiveListing;
import com.retrocrawler.core.archive.source.ArchiveSession;
import com.retrocrawler.core.archive.source.ArchiveSource;
import com.retrocrawler.core.stash.ArchiveGear;
import com.retrocrawler.core.stash.Stash;

class RetroCrawlerMultiArchiveTest {

	private static final Path FIRST_ROOT = Path.of("remote/first");

	private static final Path SECOND_ROOT = Path.of("remote/second");

	private static final ArchiveDescriptor FIRST = descriptor("first", FIRST_ROOT);

	private static final ArchiveDescriptor SECOND = descriptor("second", SECOND_ROOT);

	@Test
	void crawlsSeveralArchivesThroughOneSharedModel() throws IOException {
		final RecordingSource firstSource = new RecordingSource();
		final RecordingSource secondSource = new RecordingSource();
		final RetroCrawler crawler = crawler(firstSource, secondSource);

		crawler.crawl(new Journal(), ReindexScope.all());

		assertEquals(List.of(FIRST_ROOT), firstSource.openedRoots);
		assertEquals(List.of(SECOND_ROOT), secondSource.openedRoots);
		assertEquals(List.of(FIRST.id(), SECOND.id()), crawler.archives().stream().map(ArchiveDescriptor::id).toList());
		assertEquals(SECOND, crawler.archive(SECOND.id()));
	}

	@Test
	void inspectsContentThroughTheSelectedArchiveSource() throws IOException {
		final RetroCrawler crawler = RetroCrawler.builder().model(model()).repository(new InMemoryRepository())
				.archive(FIRST, contentSource(FIRST_ROOT, "first evidence"))
				.archive(SECOND, contentSource(SECOND_ROOT, "second evidence")).build();

		final Optional<String> first = crawler.inspect(ari(FIRST, Path.of("evidence.txt")),
				RetroCrawlerMultiArchiveTest::readString);
		final Optional<String> second = crawler.inspect(ari(SECOND, Path.of("evidence.txt")),
				RetroCrawlerMultiArchiveTest::readString);

		assertEquals(Optional.of("first evidence"), first);
		assertEquals(Optional.of("second evidence"), second);
	}

	@Test
	void crawlsEveryRegisteredArchiveInOnePass() throws IOException {
		final RecordingSource firstSource = new RecordingSource();
		final RecordingSource secondSource = new RecordingSource();
		final RetroCrawler crawler = crawler(firstSource, secondSource);

		final Stash stash = crawler.crawl(new Journal(), ReindexScope.all());

		assertEquals(List.of(FIRST_ROOT), firstSource.openedRoots);
		assertEquals(List.of(SECOND_ROOT), secondSource.openedRoots);
		assertEquals(List.of(FIRST, SECOND), stash.archives().stream().map(ArchiveGear::archive).toList());
	}

	@Test
	void failLateVisitsEveryArchiveAndReportsEveryFinderException() {
		final RecordingSource firstSource = new RecordingSource();
		final RecordingSource secondSource = new RecordingSource();
		final InMemoryRepository repository = new InMemoryRepository();
		final Model failingModel = Model
				.from(Set.of(FailingArchiveConfiguration.class, RetroCrawlerBuilderTest.TestGear.class));
		final RetroCrawler crawler = RetroCrawler.builder().model(failingModel).repository(repository)
				.archive(FIRST, firstSource).archive(SECOND, secondSource).build();
		final Journal journal = new Journal(FailureMode.FAIL_LATE);

		final CrawlException report = assertThrows(CrawlException.class,
				() -> crawler.crawl(journal, ReindexScope.all()));

		assertEquals(List.of(FIRST_ROOT), firstSource.openedRoots);
		assertEquals(List.of(SECOND_ROOT), secondSource.openedRoots);
		assertEquals(2, report.failures().size());
		assertEquals(List.of(FIRST.id(), SECOND.id()), report.failures().stream().map(ClueFindingException.class::cast)
				.map(failure -> failure.archiveId().orElseThrow()).toList());
		assertTrue(repository.retrieve(FIRST.id()).isEmpty());
		assertTrue(repository.retrieve(SECOND.id()).isEmpty());
	}

	@Test
	void routesASubtreeReindexToTheArchiveThatContainsIt() throws IOException {
		final RecordingSource firstSource = new RecordingSource();
		final RecordingSource secondSource = new RecordingSource();
		final RetroCrawler crawler = crawler(firstSource, secondSource);
		crawler.crawl(new Journal(), ReindexScope.all());
		firstSource.openedRoots.clear();
		secondSource.openedRoots.clear();

		crawler.crawl(new Journal(), ReindexScope.subtree(ari(SECOND, Path.of(""))));

		assertTrue(firstSource.openedRoots.isEmpty());
		assertEquals(List.of(SECOND_ROOT), secondSource.openedRoots);
	}

	@Test
	void rejectsASubtreeInAnUnknownArchive() {
		final RetroCrawler crawler = crawler(new RecordingSource(), new RecordingSource());
		final ARI unknown = ARI.of(crawler.collectionId(), ArchiveId.of("third"), Path.of("subtree"));

		final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
				() -> crawler.crawl(new Journal(), ReindexScope.subtree(unknown)));

		assertEquals("Unknown archive: third", failure.getMessage());
	}

	@Test
	void rejectsDuplicateArchiveIds() {
		final RetroCrawler.Builder duplicate = RetroCrawler.builder().archive(FIRST, new RecordingSource());

		final IllegalArgumentException duplicateFailure = assertThrows(IllegalArgumentException.class,
				() -> duplicate.archive(descriptor("first", SECOND_ROOT), new RecordingSource()));

		assertEquals("Archive is already configured: first", duplicateFailure.getMessage());
	}

	@Test
	void usesTheFilesystemSourceForAnArchiveRegisteredWithoutAProvider(@TempDir final Path root) throws IOException {
		final ArchiveDescriptor archive = descriptor("filesystem", root);
		final RetroCrawler crawler = RetroCrawler.builder().model(model()).repository(new InMemoryRepository())
				.archive(archive).build();

		crawler.crawl(new Journal(), ReindexScope.all());

		assertEquals(List.of(archive), crawler.archives());
	}

	@Test
	void rejectsUnknownArchiveIds() {
		final RetroCrawler crawler = crawler(new RecordingSource(), new RecordingSource());

		final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
				() -> crawler.archive(ArchiveId.of("missing")));

		assertEquals("Unknown archive: missing", failure.getMessage());
	}

	private static RetroCrawler crawler(final ArchiveSource firstSource, final ArchiveSource secondSource) {
		return RetroCrawler.builder().model(model()).repository(new InMemoryRepository()).archive(FIRST, firstSource)
				.archive(SECOND, secondSource).build();
	}

	private static Model model() {
		return Model.from(
				Set.of(RetroCrawlerBuilderTest.TestArchiveConfiguration.class, RetroCrawlerBuilderTest.TestGear.class));
	}

	private static ArchiveDescriptor descriptor(final String id, final Path root) {
		return new ArchiveDescriptor(ArchiveId.of(id), id, root);
	}

	private static ARI ari(final ArchiveDescriptor archive, final Path resourcePath) {
		return ARI.of("factory_test", archive.id(), resourcePath);
	}

	private static ArchiveSource contentSource(final Path root, final String value) {
		return ignored -> new ArchiveSession() {

			private final ArchiveFolder folder = () -> root;

			private final ArchiveFile file = () -> root.resolve("evidence.txt");

			@Override
			public ArchiveFolder root() {
				return folder;
			}

			@Override
			public ArchiveListing list(final ArchiveFolder ignoredFolder) {
				return new ArchiveListing(List.of(), List.of(file));
			}

			@Override
			public <T> Optional<T> access(final ArchiveFile ignoredFile, final ArchiveFileAccessor<T> accessor)
					throws IOException {
				try (InputStream content = new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8))) {
					return Optional.of(Objects.requireNonNull(accessor.access(content), "accessor result"));
				}
			}

			@Override
			public void close() {
				// no-op
			}
		};
	}

	private static String readString(final InputStream content) throws IOException {
		return new String(content.readAllBytes(), StandardCharsets.UTF_8);
	}

	@RetroCollection(id = "failing_multi_archive_test")
	@RetroClues(ThrowingClueFinder.class)
	public static class FailingArchiveConfiguration {
	}

	public static class ThrowingClueFinder implements ClueFinder {

		@Override
		public Clues find(final ArchiveFolderView folder) {
			final String folderName = folder.name();
			throw new IllegalStateException("Unexpected clue-finder failure at " + folderName);
		}
	}

	private static final class RecordingSource implements ArchiveSource {

		private final List<Path> openedRoots = new ArrayList<>();

		@Override
		public ArchiveSession open(final Path root) {
			openedRoots.add(root);
			return new ArchiveSession() {

				private final ArchiveFolder folder = () -> root;

				@Override
				public ArchiveFolder root() {
					return folder;
				}

				@Override
				public ArchiveListing list(final ArchiveFolder ignored) {
					return new ArchiveListing(List.of(), List.of());
				}

				@Override
				public <T> Optional<T> access(final ArchiveFile file, final ArchiveFileAccessor<T> accessor) {
					throw new AssertionError("No files exist in this source.");
				}

				@Override
				public void close() {
					// no-op
				}
			};
		}
	}
}
