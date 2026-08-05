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
import com.retrocrawler.core.annotation.RetroClues;
import com.retrocrawler.core.annotation.RetroCollection;
import com.retrocrawler.core.annotation.RetroFact;
import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.core.annotation.RetroId;
import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.archive.ReindexScope;
import com.retrocrawler.core.archive.Repository;
import com.retrocrawler.core.archive.clues.Archive;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.Clues;
import com.retrocrawler.core.archive.clues.FolderNameClueFinder;
import com.retrocrawler.core.gear.GearTreeFactory;
import com.retrocrawler.core.gear.matcher.AnyGearMatcher;
import com.retrocrawler.core.progress.ProgressAccuracy;
import com.retrocrawler.core.progress.ProgressSnapshot;
import com.retrocrawler.core.progress.ProgressStage;
import com.retrocrawler.core.progress.ProgressState;
import com.retrocrawler.core.progress.Progressor;
import com.retrocrawler.core.util.RetroAttribute;

class RetroIdValidationTest {

	private static final ArchiveId ARCHIVE_ID = ArchiveId.of("test_archive");

	private static final Progressor SILENT_PROGRESSOR = new Progressor();

	@TempDir
	private Path archiveRoot;

	@Test
	void permitsMissingOptionalFactBackedRetroId() throws IOException {
		Files.createDirectories(archiveRoot.resolve("gear-without-id"));

		final List<TestGear> gear = crawler().crawlAllGear(SILENT_PROGRESSOR, ReindexScope.all(), TestGear.class);

		assertEquals(1, gear.size());
		assertNull(gear.getFirst().catalogId);
	}

	@Test
	void reportsEverySourceAriForDuplicateRetroId() throws IOException {
		Files.createDirectories(archiveRoot.resolve("id-200001"));
		Files.createDirectories(archiveRoot.resolve("nested").resolve("id-200001"));
		final RecordingFactory factory = new RecordingFactory();

		final DuplicateRetroIdException failure = assertThrows(DuplicateRetroIdException.class,
				() -> crawler().crawlAll(SILENT_PROGRESSOR, ReindexScope.all(), factory));

		final List<String> paths = failure.duplicates().get("200001");
		assertEquals(2, paths.size());
		assertTrue(paths.contains(ARI.of("retro_id_validation", ARCHIVE_ID, Path.of("id-200001")).toString()));
		assertTrue(
				paths.contains(ARI.of("retro_id_validation", ARCHIVE_ID, Path.of("nested", "id-200001")).toString()));
		assertTrue(failure.getMessage().contains("200001"));
		assertEquals(0, factory.beginArchiveCount);
	}

	@Test
	void reportsADuplicateRetroIdAcrossSeveralArchives() throws IOException {
		final Path firstRoot = Files.createDirectories(archiveRoot.resolve("first"));
		final Path secondRoot = Files.createDirectories(archiveRoot.resolve("second"));
		Files.createDirectories(firstRoot.resolve("id-200001"));
		Files.createDirectories(secondRoot.resolve("id-200001"));
		final Model model = Model.from(Set.of(TestArchive.class, TestGear.class));
		final RetroCrawler crawler = RetroCrawler.builder().model(model).repository(new MemoryRepository())
				.archive(ArchiveDescriptor.of(ArchiveId.of("first"), firstRoot))
				.archive(ArchiveDescriptor.of(ArchiveId.of("second"), secondRoot)).build();

		final DuplicateRetroIdException failure = assertThrows(DuplicateRetroIdException.class,
				() -> crawler.crawlAllGear(SILENT_PROGRESSOR, ReindexScope.all(), TestGear.class));

		final List<String> paths = failure.duplicates().get("200001");
		assertEquals(2, paths.size());
		assertTrue(
				paths.contains(ARI.of("retro_id_validation", ArchiveId.of("first"), Path.of("id-200001")).toString()));
		assertTrue(
				paths.contains(ARI.of("retro_id_validation", ArchiveId.of("second"), Path.of("id-200001")).toString()));
	}

	@Test
	void acceptsADuplicateRetroIdWhenOnlyOneArchiveIsCrawled() throws IOException {
		final Path firstRoot = Files.createDirectories(archiveRoot.resolve("first"));
		final Path secondRoot = Files.createDirectories(archiveRoot.resolve("second"));
		Files.createDirectories(firstRoot.resolve("id-200001"));
		Files.createDirectories(secondRoot.resolve("id-200001"));
		final Model model = Model.from(Set.of(TestArchive.class, TestGear.class));
		final ArchiveId firstId = ArchiveId.of("first");
		final RetroCrawler crawler = RetroCrawler.builder().model(model).repository(new MemoryRepository())
				.archive(ArchiveDescriptor.of(firstId, firstRoot))
				.archive(ArchiveDescriptor.of(ArchiveId.of("second"), secondRoot)).build();

		final List<TestGear> gear = crawler.crawlGear(firstId, SILENT_PROGRESSOR, ReindexScope.all(), TestGear.class);

		assertEquals(1, gear.size());
	}

	@Test
	void reportsExactProgressWhileResolvingTheExtractedArchive() throws IOException {
		Files.createDirectories(archiveRoot.resolve("id-200001"));
		Files.createDirectories(archiveRoot.resolve("id-200002"));
		final List<ProgressSnapshot> events = new java.util.ArrayList<>();

		crawler().crawlAllGear(Progressor.observing(events::add), ReindexScope.all(), TestGear.class);

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
		final Model model = Model.from(Set.of(TestArchive.class, TestGear.class));
		return RetroCrawler.builder().model(model).repository(new MemoryRepository())
				.archive(ArchiveDescriptor.of(ARCHIVE_ID, archiveRoot)).build();
	}

	@RetroCollection(id = "retro_id_validation")
	@RetroClues(fromFolderName = TestClueFinder.class)
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

	public static final class TestClueFinder implements FolderNameClueFinder {

		@Override
		public Clues find(final String folderName) {
			if (folderName.startsWith("id-")) {
				return Clues.of(Clue.of("catalogId", folderName.substring("id-".length())));
			}
			if (folderName.equals("gear-without-id")) {
				return Clues.of(Clue.of("tag", "gear"));
			}
			return Clues.none();
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

		private int beginArchiveCount;

		@Override
		public Class<TestGear> gearType() {
			return TestGear.class;
		}

		@Override
		public void beginArchive(final ArchiveDescriptor archive) {
			beginArchiveCount++;
		}

		@Override
		public void endArchive(final ArchiveDescriptor archive) {
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
