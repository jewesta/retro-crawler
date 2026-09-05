package com.retrocrawler.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
import com.retrocrawler.core.annotation.RetroAnyGear;
import com.retrocrawler.core.annotation.RetroClues;
import com.retrocrawler.core.annotation.RetroCollection;
import com.retrocrawler.core.annotation.RetroFact;
import com.retrocrawler.core.annotation.RetroId;
import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.archive.ReindexScope;
import com.retrocrawler.core.archive.Repository;
import com.retrocrawler.core.archive.clues.Archive;
import com.retrocrawler.core.archive.clues.ArchiveFolderView;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.ClueFinder;
import com.retrocrawler.core.archive.clues.Clues;
import com.retrocrawler.core.progress.ProgressAccuracy;
import com.retrocrawler.core.progress.ProgressSnapshot;
import com.retrocrawler.core.progress.ProgressState;
import com.retrocrawler.core.progress.Progressor;
import com.retrocrawler.core.util.RetroAttribute;

class RetroIdValidationTest {

	private static final ArchiveId ARCHIVE_ID = ArchiveId.of("test_archive");

	@TempDir
	private Path archiveRoot;

	@Test
	void permitsMissingOptionalFactBackedRetroId() throws IOException {
		Files.createDirectories(archiveRoot.resolve("gear-without-id"));

		final List<TestGear> gear = crawler().crawl(new Journal(), ReindexScope.all()).query(TestGear.class).pull()
				.gear();

		assertEquals(1, gear.size());
		assertNull(gear.getFirst().catalogId);
	}

	@Test
	void reportsEverySourceAriForDuplicateRetroId() throws IOException {
		Files.createDirectories(archiveRoot.resolve("id-200001"));
		Files.createDirectories(archiveRoot.resolve("nested").resolve("id-200001"));
		final DuplicateRetroIdException failure = assertThrows(DuplicateRetroIdException.class,
				() -> crawler().crawl(new Journal(), ReindexScope.all()));

		final List<String> paths = failure.duplicates().get("200001");
		assertEquals(2, paths.size());
		assertTrue(paths.contains(ARI.of("retro_id_validation", ARCHIVE_ID, Path.of("id-200001")).toString()));
		assertTrue(
				paths.contains(ARI.of("retro_id_validation", ARCHIVE_ID, Path.of("nested", "id-200001")).toString()));
		assertTrue(failure.getMessage().contains("200001"));
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
				() -> crawler.crawl(new Journal(), ReindexScope.all()));

		final List<String> paths = failure.duplicates().get("200001");
		assertEquals(2, paths.size());
		assertTrue(
				paths.contains(ARI.of("retro_id_validation", ArchiveId.of("first"), Path.of("id-200001")).toString()));
		assertTrue(
				paths.contains(ARI.of("retro_id_validation", ArchiveId.of("second"), Path.of("id-200001")).toString()));
	}

	@Test
	void failLateRecordsDuplicateRetroIdsAndDoesNotEmitAPartialResult() throws IOException {
		Files.createDirectories(archiveRoot.resolve("id-200001"));
		Files.createDirectories(archiveRoot.resolve("nested").resolve("id-200001"));
		final Journal journal = new Journal(FailureMode.FAIL_LATE);

		final CrawlException report = assertThrows(CrawlException.class,
				() -> crawler().crawl(journal, ReindexScope.all()));

		assertEquals(1, report.failures().size());
		assertInstanceOf(DuplicateRetroIdException.class, report.failures().getFirst());
		assertEquals(report.failures(), journal.failures());
	}

	@Test
	void reportsExactProgressWhileResolvingTheExtractedArchive() throws IOException {
		Files.createDirectories(archiveRoot.resolve("id-200001"));
		Files.createDirectories(archiveRoot.resolve("id-200002"));
		final List<ProgressSnapshot> events = new java.util.ArrayList<>();

		crawler().crawl(new Journal(Progressor.observing(progress -> events.add(progress.snapshot()))),
				ReindexScope.all());

		final List<ProgressSnapshot> resolving = events.stream()
				.filter(event -> event.stage().equals(CrawlProgressStages.RESOLVING)).toList();
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
	@RetroClues(TestClueFinder.class)
	public static final class TestArchive {

		private TestArchive() {
		}
	}

	@RetroAnyGear
	public static final class TestGear {

		@RetroId
		@RetroFact(optional = true)
		private String catalogId;

		@RetroAnyAttribute
		private final Map<String, RetroAttribute> attributes = new HashMap<>();

		public TestGear() {
		}
	}

	public static final class TestClueFinder implements ClueFinder {

		@Override
		public Clues find(final ArchiveFolderView folder) {
			final String folderName = folder.name();
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

}
