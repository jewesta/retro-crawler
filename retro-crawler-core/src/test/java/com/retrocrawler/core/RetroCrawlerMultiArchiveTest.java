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

import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.archive.InMemoryRepository;
import com.retrocrawler.core.archive.ReindexScope;
import com.retrocrawler.core.archive.source.ArchiveFile;
import com.retrocrawler.core.archive.source.ArchiveFileAccessor;
import com.retrocrawler.core.archive.source.ArchiveFolder;
import com.retrocrawler.core.archive.source.ArchiveListing;
import com.retrocrawler.core.archive.source.ArchiveSession;
import com.retrocrawler.core.archive.source.ArchiveSource;
import com.retrocrawler.core.progress.Progressor;

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

		crawler.crawlGear(FIRST.id(), new Progressor(), ReindexScope.all(), RetroCrawlerBuilderTest.TestGear.class);

		assertEquals(List.of(FIRST_ROOT), firstSource.openedRoots);
		assertTrue(secondSource.openedRoots.isEmpty());

		crawler.crawlGear(SECOND.id(), new Progressor(), ReindexScope.all(), RetroCrawlerBuilderTest.TestGear.class);

		assertEquals(List.of(SECOND_ROOT), secondSource.openedRoots);
		assertEquals(List.of(FIRST.id(), SECOND.id()), crawler.archives().stream().map(ArchiveDescriptor::id).toList());
		assertEquals(SECOND, crawler.archive(SECOND.id()));
	}

	@Test
	void inspectsContentThroughTheSelectedArchiveSource() throws IOException {
		final RetroCrawler crawler = RetroCrawler.builder().model(model()).repository(new InMemoryRepository())
				.archive(FIRST, contentSource(FIRST_ROOT, "first evidence"))
				.archive(SECOND, contentSource(SECOND_ROOT, "second evidence")).build();

		final Optional<String> first = crawler.inspect(FIRST.id(), FIRST_ROOT.resolve("evidence.txt"),
				RetroCrawlerMultiArchiveTest::readString);
		final Optional<String> second = crawler.inspect(SECOND.id(), SECOND_ROOT.resolve("evidence.txt"),
				RetroCrawlerMultiArchiveTest::readString);

		assertEquals(Optional.of("first evidence"), first);
		assertEquals(Optional.of("second evidence"), second);
	}

	@Test
	void requiresArchiveSelectionWhenSeveralArchivesAreConfigured() {
		final RetroCrawler crawler = crawler(new RecordingSource(), new RecordingSource());

		final IllegalStateException descriptorFailure = assertThrows(IllegalStateException.class,
				crawler::archiveDescriptor);
		final IllegalStateException crawlFailure = assertThrows(IllegalStateException.class,
				() -> crawler.crawlGear(new Progressor(), ReindexScope.none(), RetroCrawlerBuilderTest.TestGear.class));

		assertEquals("Expected one archive but this crawler has 2. Select an archive by id.",
				descriptorFailure.getMessage());
		assertEquals(descriptorFailure.getMessage(), crawlFailure.getMessage());
	}

	@Test
	void rejectsDuplicateArchiveIdsAndMixedDefaultSourceComposition() {
		final RetroCrawler.Builder duplicate = RetroCrawler.builder().archive(FIRST, new RecordingSource());

		final IllegalArgumentException duplicateFailure = assertThrows(IllegalArgumentException.class,
				() -> duplicate.archive(descriptor("first", SECOND_ROOT), new RecordingSource()));
		final RetroCrawler.Builder mixed = RetroCrawler.builder().archiveSource(new RecordingSource());
		final IllegalStateException mixedFailure = assertThrows(IllegalStateException.class,
				() -> mixed.archive(FIRST, new RecordingSource()));
		final RetroCrawler.Builder reverseMixed = RetroCrawler.builder().archive(FIRST, new RecordingSource());
		final IllegalStateException reverseMixedFailure = assertThrows(IllegalStateException.class,
				() -> reverseMixed.archiveSource(new RecordingSource()));

		assertEquals("Archive is already configured: first", duplicateFailure.getMessage());
		assertEquals("Explicit archives cannot be combined with a default archive source.", mixedFailure.getMessage());
		assertEquals("A default archive source cannot be combined with explicit archives.",
				reverseMixedFailure.getMessage());
	}

	@Test
	void usesTheFilesystemSourceForAnArchiveRegisteredWithoutAProvider(@TempDir final Path root) throws IOException {
		final ArchiveDescriptor archive = descriptor("filesystem", root);
		final RetroCrawler crawler = RetroCrawler.builder().model(model()).repository(new InMemoryRepository())
				.archive(archive).build();

		crawler.crawlGear(archive.id(), new Progressor(), ReindexScope.all(), RetroCrawlerBuilderTest.TestGear.class);

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
		return new ArchiveDescriptor(ArchiveId.of(id), id, List.of(root));
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
