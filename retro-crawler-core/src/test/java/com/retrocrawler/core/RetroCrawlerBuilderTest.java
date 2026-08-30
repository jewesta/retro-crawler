package com.retrocrawler.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.retrocrawler.core.annotation.RetroAnyAttribute;
import com.retrocrawler.core.annotation.RetroClues;
import com.retrocrawler.core.annotation.RetroCollection;
import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.core.annotation.RetroSource;
import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.archive.CrawlPlanning;
import com.retrocrawler.core.archive.InMemoryRepository;
import com.retrocrawler.core.archive.ReindexScope;
import com.retrocrawler.core.archive.Repository;
import com.retrocrawler.core.archive.clues.Archive;
import com.retrocrawler.core.archive.clues.ArchiveFolderView;
import com.retrocrawler.core.archive.clues.ArchiveNode;
import com.retrocrawler.core.archive.clues.Artifact;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.ClueFinder;
import com.retrocrawler.core.archive.clues.Clues;
import com.retrocrawler.core.archive.source.ArchiveFile;
import com.retrocrawler.core.archive.source.ArchiveFileAccessor;
import com.retrocrawler.core.archive.source.ArchiveFolder;
import com.retrocrawler.core.archive.source.ArchiveListing;
import com.retrocrawler.core.archive.source.ArchiveSession;
import com.retrocrawler.core.archive.source.ArchiveSource;
import com.retrocrawler.core.gear.matcher.AnyGearMatcher;
import com.retrocrawler.core.stash.Stash;
import com.retrocrawler.core.util.RetroAttribute;

class RetroCrawlerBuilderTest {

	private static final Path ROOT = Path.of("/this/path/must/not/be/crawled");

	private static final ArchiveDescriptor ARCHIVE = ArchiveDescriptor.of(ArchiveId.of("factory_test"), ROOT);

	@Test
	void suppliesConfiguredRepositoryToCrawler() throws IOException {
		final RecordingRepository repository = new RecordingRepository();
		final Model model = Model.from(Set.of(TestArchiveConfiguration.class, TestGear.class));
		final RetroCrawler crawler = RetroCrawler.builder().model(model).repository(repository).archive(ARCHIVE)
				.build();

		final Stash result = crawler.access(new Journal());
		final Stash accessedAgain = crawler.access(new Journal());

		assertEquals(1, repository.retrieveCount);
		assertSame(result, accessedAgain);
		assertEquals(1, result.archives().size());
		assertEquals(ARCHIVE, result.archives().getFirst().archive());
	}

	@Test
	void configuresConventionalRepositoryAndArchivesFromLocations(@TempDir final Path temporaryDirectory)
			throws IOException {
		final Path repositoryRoot = temporaryDirectory.resolve("repository");
		final Path archiveRoot = Files.createDirectory(temporaryDirectory.resolve("archive"));
		final ArchiveDescriptor archive = ArchiveDescriptor.of(ArchiveId.of("located"), archiveRoot);
		final Model model = Model.from(Set.of(TestArchiveConfiguration.class, TestGear.class));
		final RetroCrawler crawler = RetroCrawler.builder().model(model)
				.locations(new Locations(repositoryRoot, List.of(archive))).build();

		crawler.crawl(new Journal(), ReindexScope.all());

		assertEquals(List.of(archive), crawler.archives());
		assertTrue(Files.exists(repositoryRoot.resolve("archive_located.json")));
	}

	@Test
	void stashRetainsTheSourceAriOfEveryGearNode() throws IOException {
		final Instant fullCrawl = Instant.parse("2026-08-29T08:00:00Z");
		final Instant fullObservation = Instant.parse("2026-08-29T08:04:00Z");
		final Instant shelfCrawl = Instant.parse("2026-08-30T09:00:00Z");
		final Instant shelfObservation = Instant.parse("2026-08-30T09:00:20Z");
		final Artifact artifact = new Artifact(Clues.of(Clue.of("name", "test gear")));
		final ArchiveNode archiveRoot = new ArchiveNode(".", fullCrawl, fullObservation, null,
				List.of(new ArchiveNode("shelf", shelfCrawl, shelfObservation, artifact, List.of()),
						new ArchiveNode("metadata", shelfCrawl, shelfObservation, null, List.of())));
		final Repository repository = new FixedArchiveRepository(
				Archive.of("factory_test", ARCHIVE.id(), ROOT, archiveRoot));
		final Model model = Model.from(Set.of(TestArchiveConfiguration.class, TestGear.class));
		final RetroCrawler crawler = RetroCrawler.builder().model(model).repository(repository).archive(ARCHIVE)
				.build();

		final Stash stash = crawler.access(new Journal());

		final var node = stash.archives().getFirst().roots().getFirst();
		assertEquals(ARI.of("factory_test", ARCHIVE.id(), Path.of("shelf")), node.source());
		assertEquals(node.source(), ((TestGear) node.gear()).source);
		assertEquals(fullCrawl, stash.crawlStartedAt(ARCHIVE.id()).orElseThrow());
		assertEquals(fullObservation, stash.observedAt(ARCHIVE.id()).orElseThrow());
		assertEquals(Duration.ofMinutes(4), stash.crawlDuration(ARCHIVE.id()).orElseThrow());
		assertEquals(shelfCrawl, stash.crawlStartedAt(node.source()).orElseThrow());
		assertEquals(shelfObservation, stash.observedAt(node.source()).orElseThrow());
		assertEquals(shelfCrawl,
				stash.crawlStartedAt(ARI.of("factory_test", ARCHIVE.id(), Path.of("metadata"))).orElseThrow());
	}

	@Test
	void requiresModel() {
		final IllegalStateException failure = assertThrows(IllegalStateException.class,
				() -> RetroCrawler.builder().repository(new RecordingRepository()).archive(ARCHIVE).build());

		assertEquals("Missing required model configuration.", failure.getMessage());
	}

	@Test
	void requiresRepository() {
		final Model model = Model.from(Set.of(TestArchiveConfiguration.class, TestGear.class));

		final IllegalStateException failure = assertThrows(IllegalStateException.class,
				() -> RetroCrawler.builder().model(model).archive(ARCHIVE).build());

		assertEquals("Missing required repository configuration.", failure.getMessage());
	}

	@Test
	void requiresAtLeastOneArchive() {
		final Model model = Model.from(Set.of(TestArchiveConfiguration.class, TestGear.class));

		final IllegalStateException failure = assertThrows(IllegalStateException.class,
				() -> RetroCrawler.builder().model(model).repository(new RecordingRepository()).build());

		assertEquals("At least one archive must be configured.", failure.getMessage());
	}

	@Test
	void rejectsDuplicateModelConfiguration() {
		final Model model = Model.from(Set.of(TestArchiveConfiguration.class, TestGear.class));
		final RetroCrawler.Builder builder = RetroCrawler.builder().model(model);

		final IllegalStateException failure = assertThrows(IllegalStateException.class, () -> builder.model(model));

		assertEquals("Model is already configured.", failure.getMessage());
	}

	@Test
	void rejectsDuplicateRepositoryConfiguration() {
		final RecordingRepository repository = new RecordingRepository();
		final RetroCrawler.Builder builder = RetroCrawler.builder().repository(repository);

		final IllegalStateException failure = assertThrows(IllegalStateException.class,
				() -> builder.repository(repository));

		assertEquals("Repository is already configured.", failure.getMessage());
	}

	@Test
	void doesNotMixLocationsWithExplicitStorageConfiguration() {
		final Locations locations = new Locations(Path.of("repository"), List.of(ARCHIVE));

		final IllegalStateException repositoryFailure = assertThrows(IllegalStateException.class,
				() -> RetroCrawler.builder().locations(locations).repository(new RecordingRepository()));
		final IllegalStateException archiveFailure = assertThrows(IllegalStateException.class,
				() -> RetroCrawler.builder().locations(locations).archive(ARCHIVE));

		assertEquals("Explicit repository configuration cannot be combined with configured locations.",
				repositoryFailure.getMessage());
		assertEquals("Explicit archive configuration cannot be combined with configured locations.",
				archiveFailure.getMessage());
	}

	@Test
	void acceptsExplicitCrawlPlanning() {
		final Model model = Model.from(Set.of(TestArchiveConfiguration.class, TestGear.class));
		final CrawlPlanning planning = new CrawlPlanning(25, 3, 200, Duration.ofSeconds(2));

		RetroCrawler.builder().model(model).repository(new RecordingRepository()).archive(ARCHIVE)
				.crawlPlanning(planning).build();
	}

	@Test
	void rejectsDuplicateCrawlPlanningConfiguration() {
		final CrawlPlanning planning = CrawlPlanning.defaults();
		final RetroCrawler.Builder builder = RetroCrawler.builder().crawlPlanning(planning);

		final IllegalStateException failure = assertThrows(IllegalStateException.class,
				() -> builder.crawlPlanning(planning));

		assertEquals("Crawl planning is already configured.", failure.getMessage());
	}

	@Test
	void acceptsAnExplicitArchiveSource() {
		final Model model = Model.from(Set.of(TestArchiveConfiguration.class, TestGear.class));
		final ArchiveSource source = root -> {
			throw new AssertionError("Stored archive should be reused without opening the source.");
		};

		RetroCrawler.builder().model(model).repository(new RecordingRepository()).archive(ARCHIVE, source).build();
	}

	@Test
	void suppliesTheConfiguredArchiveSourceToTheCrawler() throws IOException {
		final Model model = Model.from(Set.of(TestArchiveConfiguration.class, TestGear.class));
		final AtomicBoolean opened = new AtomicBoolean();
		final AtomicBoolean closed = new AtomicBoolean();
		final ArchiveSource source = root -> {
			opened.set(true);
			final ArchiveFolder folder = () -> root;
			return new ArchiveSession() {

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
					throw new AssertionError("No files exist in this archive source.");
				}

				@Override
				public void close() {
					closed.set(true);
				}
			};
		};
		final RetroCrawler crawler = RetroCrawler.builder().model(model).repository(new InMemoryRepository())
				.archive(ARCHIVE, source).build();

		final Stash first = crawler.crawl(new Journal(), ReindexScope.all());
		assertSame(first, crawler.access(new Journal()));
		final Stash second = crawler.crawl(new Journal(), ReindexScope.all());
		assertNotSame(first, second);
		assertSame(second, crawler.access(new Journal()));

		assertTrue(opened.get());
		assertTrue(closed.get());
	}

	@Test
	void requiresAPhysicalScopeForCrawl() {
		final Model model = Model.from(Set.of(TestArchiveConfiguration.class, TestGear.class));
		final RetroCrawler crawler = RetroCrawler.builder().model(model).repository(new RecordingRepository())
				.archive(ARCHIVE).build();

		final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
				() -> crawler.crawl(new Journal(), ReindexScope.none()));

		assertEquals("crawl requires a physical reindex scope; use access to reuse stored clues.",
				failure.getMessage());
	}

	@Test
	void rejectsDuplicateArchiveConfiguration() {
		final RetroCrawler.Builder builder = RetroCrawler.builder().archive(ARCHIVE);

		final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
				() -> builder.archive(ARCHIVE));

		assertEquals("Archive is already configured: factory_test", failure.getMessage());
	}

	@Test
	void exposesRegisteredArchivesInCompositionOrder() {
		final Model model = Model.from(Set.of(TestArchiveConfiguration.class, TestGear.class));
		final ArchiveDescriptor second = ArchiveDescriptor.of(ArchiveId.of("second"), Path.of("/second"));
		final RetroCrawler crawler = RetroCrawler.builder().model(model).repository(new RecordingRepository())
				.archive(ARCHIVE).archive(second).build();

		assertEquals(List.of(ARCHIVE, second), crawler.archives());
		assertEquals(second, crawler.archive(ArchiveId.of("second")));
	}

	@RetroCollection(id = "factory_test")
	@RetroClues(EmptyClueFinder.class)
	public static class TestArchiveConfiguration {
	}

	@RetroGear(AnyGearMatcher.class)
	public static class TestGear {

		@RetroSource
		private ARI source;

		@RetroAnyAttribute
		private final Map<String, RetroAttribute> attributes = new HashMap<>();

		public TestGear() {
		}
	}

	public static class EmptyClueFinder implements ClueFinder {

		@Override
		public Clues find(final ArchiveFolderView folder) {
			return Clues.none();
		}
	}

	private static final class RecordingRepository implements Repository {

		private int retrieveCount;

		@Override
		public void stowaway(final Archive archive) {
			throw new AssertionError("Archive should have been retrieved without crawling.");
		}

		@Override
		public Optional<Archive> retrieve(final ArchiveId id) {
			retrieveCount++;
			return Optional.of(Archive.of("factory_test", id, ROOT, new ArchiveNode(".", null, null)));
		}
	}

	private record FixedArchiveRepository(Archive archive) implements Repository {

		@Override
		public void stowaway(final Archive ignored) {
			throw new AssertionError("Archive should have been retrieved without crawling.");
		}

		@Override
		public Optional<Archive> retrieve(final ArchiveId id) {
			return Optional.of(archive);
		}
	}

}
