package com.retrocrawler.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.retrocrawler.core.annotation.RetroAnyAttribute;
import com.retrocrawler.core.annotation.RetroArchive;
import com.retrocrawler.core.annotation.RetroFact;
import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.core.annotation.RetroId;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.archive.ArchiveRoots;
import com.retrocrawler.core.archive.Repository;
import com.retrocrawler.core.archive.clues.Archive;
import com.retrocrawler.core.archive.clues.Bucket;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.PathNameClueFinder;
import com.retrocrawler.core.gear.matcher.AnyGearMatcher;
import com.retrocrawler.core.progress.ProgressAccuracy;
import com.retrocrawler.core.progress.ProgressSnapshot;
import com.retrocrawler.core.progress.ProgressStage;
import com.retrocrawler.core.progress.ProgressState;
import com.retrocrawler.core.progress.Progressor;
import com.retrocrawler.core.util.RetroAttribute;

class RetroIdValidationTest {

	private static final Progressor SILENT_PROGRESSOR = new Progressor();

	@TempDir
	private Path archiveRoot;

	@Test
	void permitsMissingOptionalFactBackedRetroId() throws IOException {
		Files.createDirectories(archiveRoot.resolve("gear-without-id"));

		final List<TestGear> gear = crawler().crawlGear(SILENT_PROGRESSOR, true, TestGear.class);

		assertEquals(1, gear.size());
		assertNull(gear.getFirst().catalogId);
	}

	@Test
	void reportsEverySourcePathForDuplicateRetroId() throws IOException {
		final Path first = Files.createDirectories(archiveRoot.resolve("id-200001"));
		final Path second = Files.createDirectories(archiveRoot.resolve("nested").resolve("id-200001"));
		final RecordingFactory factory = new RecordingFactory();

		final DuplicateRetroIdException failure = assertThrows(DuplicateRetroIdException.class,
				() -> crawler().crawl(SILENT_PROGRESSOR, true, factory));

		final List<String> paths = failure.getDuplicates().get("200001");
		assertEquals(2, paths.size());
		assertTrue(paths.contains(first.toString()));
		assertTrue(paths.contains(second.toString()));
		assertTrue(failure.getMessage().contains("200001"));
		assertEquals(0, factory.beginBucketCount);
	}

	@Test
	void reportsExactProgressWhileResolvingTheExtractedArchive() throws IOException {
		Files.createDirectories(archiveRoot.resolve("id-200001"));
		Files.createDirectories(archiveRoot.resolve("id-200002"));
		final List<ProgressSnapshot> events = new java.util.ArrayList<>();

		crawler().crawlGear(Progressor.observing(events::add), true, TestGear.class);

		final List<ProgressSnapshot> resolving = events.stream()
				.filter(event -> event.stage().equals(ProgressStage.RESOLVING)).toList();
		assertEquals(0, resolving.getFirst().completed());
		assertEquals(2, resolving.getFirst().total());
		assertEquals(2, resolving.getLast().completed());
		assertEquals(2, resolving.getLast().total());
		assertTrue(resolving.stream().allMatch(event -> event.accuracy() == ProgressAccuracy.EXACT));
		assertEquals(ProgressState.COMPLETE, events.getLast().state());
	}

	private RetroCrawler crawler() {
		final Model model = Model.from(Set.of(TestArchive.class, TestGear.class), ArchiveRoots.from(archiveRoot));
		return RetroCrawler.builder().model(model).repository(new MemoryRepository()).build();
	}

	@RetroArchive(id = "retro_id_validation",
			findClues = @RetroArchive.LookAt(pathName = TestClueFinder.class))
	public static final class TestArchive {

		private TestArchive() {
		}
	}

	@RetroGear(AnyGearMatcher.class)
	public static final class TestGear {

		@RetroId
		@RetroFact(optional = true)
		private String catalogId;

		@RetroAnyAttribute
		private final Map<String, RetroAttribute> attributes = new HashMap<>();

		public TestGear() {
		}
	}

	public static final class TestClueFinder implements PathNameClueFinder {

		@Override
		public Set<Clue> find(final String pathName) {
			if (pathName.startsWith("id-")) {
				return Set.of(Clue.of("catalogId", pathName.substring("id-".length())));
			}
			if (pathName.equals("gear-without-id")) {
				return Set.of(Clue.of("tag", "gear"));
			}
			return Set.of();
		}
	}

	private static final class MemoryRepository implements Repository {

		private Archive archive;

		@Override
		public void stowaway(final Archive archive) {
			this.archive = archive;
		}

		@Override
		public Optional<Archive> retrieve(final ArchiveId id) {
			return Optional.ofNullable(archive);
		}
	}

	private static final class RecordingFactory implements GearTreeFactory<List<TestGear>, TestGear, TestGear> {

		private int beginBucketCount;

		@Override
		public Class<TestGear> gearType() {
			return TestGear.class;
		}

		@Override
		public void beginBucket(final Bucket bucket) {
			beginBucketCount++;
		}

		@Override
		public void endBucket(final Bucket bucket) {
			// Nothing to record.
		}

		@Override
		public TestGear addNode(final TestGear parent, final TestGear gear) {
			return gear;
		}

		@Override
		public List<TestGear> build() {
			return List.of();
		}
	}
}
