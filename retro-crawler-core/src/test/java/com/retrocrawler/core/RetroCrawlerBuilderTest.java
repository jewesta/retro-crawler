package com.retrocrawler.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.annotation.RetroAnyAttribute;
import com.retrocrawler.core.annotation.RetroArchive;
import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.archive.Repository;
import com.retrocrawler.core.archive.clues.Archive;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.PathNameClueFinder;
import com.retrocrawler.core.gear.matcher.AnyGearMatcher;
import com.retrocrawler.core.util.Monitor;
import com.retrocrawler.core.util.RetroAttribute;

class RetroCrawlerBuilderTest {

	@Test
	void suppliesConfiguredRepositoryToCrawler() throws IOException {
		final RecordingRepository repository = new RecordingRepository();
		final Model model = Model.from(Set.of(TestArchiveConfiguration.class, TestGear.class));
		final RetroCrawler crawler = RetroCrawler.builder().model(model).repository(repository).build();

		final GearArchive<TestGear> result = crawler.crawlArchive(new Monitor(message -> {
			// No progress reporting required in tests.
		}), false, TestGear.class);

		assertEquals(1, repository.retrieveCount);
		assertEquals(0, result.getBuckets().size());
	}

	@Test
	void requiresModel() {
		final IllegalStateException failure = assertThrows(IllegalStateException.class,
				() -> RetroCrawler.builder().repository(new RecordingRepository()).build());

		assertEquals("Missing required model configuration.", failure.getMessage());
	}

	@Test
	void requiresRepository() {
		final Model model = Model.from(Set.of(TestArchiveConfiguration.class, TestGear.class));

		final IllegalStateException failure = assertThrows(IllegalStateException.class,
				() -> RetroCrawler.builder().model(model).build());

		assertEquals("Missing required repository configuration.", failure.getMessage());
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

	@RetroArchive(id = "factory_test", locations = "/this/path/must/not/be/crawled",
			findClues = @RetroArchive.LookAt(pathName = EmptyClueFinder.class))
	public static class TestArchiveConfiguration {
	}

	@RetroGear(AnyGearMatcher.class)
	public static class TestGear {

		@RetroAnyAttribute
		private final Map<String, RetroAttribute> attributes = new HashMap<>();

		public TestGear() {
		}
	}

	public static class EmptyClueFinder implements PathNameClueFinder {

		@Override
		public Set<Clue> find(final String pathName) {
			return Set.of();
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
			return Optional.of(Archive.of(id, List.of()));
		}
	}

}
