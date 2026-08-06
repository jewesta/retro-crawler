package com.retrocrawler.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.annotation.RetroAnyAttribute;
import com.retrocrawler.core.annotation.RetroClues;
import com.retrocrawler.core.annotation.RetroCollection;
import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.archive.CrawlPlanning;
import com.retrocrawler.core.archive.InMemoryRepository;
import com.retrocrawler.core.archive.ReindexScope;
import com.retrocrawler.core.archive.Repository;
import com.retrocrawler.core.archive.clues.Archive;
import com.retrocrawler.core.archive.clues.ArchiveNode;
import com.retrocrawler.core.archive.clues.Artifact;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.Clues;
import com.retrocrawler.core.archive.clues.FolderNameClueFinder;
import com.retrocrawler.core.archive.source.ArchiveFile;
import com.retrocrawler.core.archive.source.ArchiveFileAccessor;
import com.retrocrawler.core.archive.source.ArchiveFolder;
import com.retrocrawler.core.archive.source.ArchiveListing;
import com.retrocrawler.core.archive.source.ArchiveSession;
import com.retrocrawler.core.archive.source.ArchiveSource;
import com.retrocrawler.core.gear.GearTreeFactory;
import com.retrocrawler.core.gear.matcher.AnyGearMatcher;
import com.retrocrawler.core.progress.ProgressState;
import com.retrocrawler.core.progress.Progressor;
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

		final Stash<TestGear> result = crawler.crawlAllStash(new Journal(), ReindexScope.none(), TestGear.class);

		assertEquals(1, repository.retrieveCount);
		assertEquals(1, result.archives().size());
		assertEquals(ARCHIVE, result.archives().getFirst().archive());
	}

	@Test
	void suppliesArtifactSourceAriToTreeFactory() throws IOException {
		final Artifact artifact = new Artifact(Clues.of(Clue.of("name", "test gear")));
		final ArchiveNode archiveRoot = new ArchiveNode(".", null,
				List.of(new ArchiveNode("shelf", artifact, List.of())));
		final Repository repository = new FixedArchiveRepository(
				Archive.of(ArchiveId.of("factory_test"), ROOT, archiveRoot));
		final Model model = Model.from(Set.of(TestArchiveConfiguration.class, TestGear.class));
		final RetroCrawler crawler = RetroCrawler.builder().model(model).repository(repository).archive(ARCHIVE)
				.build();
		final List<ARI> sources = new ArrayList<>();
		final GearTreeFactory<List<ARI>, TestGear, TestGear> factory = new GearTreeFactory<>() {

			@Override
			public Class<TestGear> gearType() {
				return TestGear.class;
			}

			@Override
			public void beginArchive(final ArchiveDescriptor archive) {
				// no-op
			}

			@Override
			public void endArchive(final ArchiveDescriptor archive) {
				// no-op
			}

			@Override
			public TestGear addNode(final TestGear parent, final TestGear gear) {
				throw new AssertionError("Expected the source-ARI overload.");
			}

			@Override
			public TestGear addNode(final TestGear parent, final TestGear gear, final ARI source) {
				sources.add(source);
				return gear;
			}

			@Override
			public List<ARI> build() {
				return List.copyOf(sources);
			}
		};

		final List<ARI> result = crawler.crawlAll(new Journal(), ReindexScope.none(), factory);

		assertEquals(List.of(ARI.of("factory_test", ARCHIVE.id(), Path.of("shelf"))), result);
	}

	@Test
	void stashRetainsTheSourceAriOfEveryGearNode() throws IOException {
		final Artifact artifact = new Artifact(Clues.of(Clue.of("name", "test gear")));
		final ArchiveNode archiveRoot = new ArchiveNode(".", null,
				List.of(new ArchiveNode("shelf", artifact, List.of())));
		final Repository repository = new FixedArchiveRepository(Archive.of(ARCHIVE.id(), ROOT, archiveRoot));
		final Model model = Model.from(Set.of(TestArchiveConfiguration.class, TestGear.class));
		final RetroCrawler crawler = RetroCrawler.builder().model(model).repository(repository).archive(ARCHIVE)
				.build();

		final Stash<TestGear> stash = crawler.crawlAllStash(new Journal(), ReindexScope.none(), TestGear.class);

		assertEquals(ARI.of("factory_test", ARCHIVE.id(), Path.of("shelf")),
				stash.archives().getFirst().roots().getFirst().source());
	}

	@Test
	void reportsFactoryFailureAsTerminalProgress() {
		final Model model = Model.from(Set.of(TestArchiveConfiguration.class, TestGear.class));
		final RetroCrawler crawler = RetroCrawler.builder().model(model).repository(new RecordingRepository())
				.archive(ARCHIVE).build();
		final Progressor progressor = Progressor.create();
		final Journal journal = new Journal(progressor);
		final GearTreeFactory<Object, Object, Object> failingFactory = new GearTreeFactory<>() {

			@Override
			public Class<Object> gearType() {
				return Object.class;
			}

			@Override
			public void beginArchive(final ArchiveDescriptor archive) {
				// Nothing to record.
			}

			@Override
			public void endArchive(final ArchiveDescriptor archive) {
				// Nothing to record.
			}

			@Override
			public Object addNode(final Object parent, final Object gear) {
				throw new AssertionError("No gear expected.");
			}

			@Override
			public Object build() {
				throw new IllegalStateException("Factory broke.");
			}
		};

		assertThrows(IllegalStateException.class, () -> crawler.crawlAll(journal, ReindexScope.none(), failingFactory));
		assertEquals(ProgressState.FAILED, progressor.snapshot().state());
		assertEquals("Crawl failed: Factory broke.", progressor.snapshot().message());
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

		crawler.crawlAllGear(new Journal(), ReindexScope.all(), TestGear.class);

		assertTrue(opened.get());
		assertTrue(closed.get());
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
	@RetroClues(fromFolderName = EmptyClueFinder.class)
	public static class TestArchiveConfiguration {
	}

	@RetroGear(AnyGearMatcher.class)
	public static class TestGear {

		@RetroAnyAttribute
		private final Map<String, RetroAttribute> attributes = new HashMap<>();

		public TestGear() {
		}
	}

	public static class EmptyClueFinder implements FolderNameClueFinder {

		@Override
		public Clues find(final String folderName) {
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
			return Optional.of(Archive.of(id, ROOT, new ArchiveNode(".", null, null)));
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
