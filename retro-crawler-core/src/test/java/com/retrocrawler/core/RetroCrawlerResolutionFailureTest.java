package com.retrocrawler.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.retrocrawler.core.annotation.RetroClues;
import com.retrocrawler.core.annotation.RetroCollection;
import com.retrocrawler.core.annotation.RetroFact;
import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.archive.InMemoryRepository;
import com.retrocrawler.core.archive.ReindexScope;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.ClueFindingException;
import com.retrocrawler.core.archive.clues.Clues;
import com.retrocrawler.core.archive.clues.DuplicateClueException;
import com.retrocrawler.core.archive.clues.FolderNameClueFinder;
import com.retrocrawler.core.gear.Confidence;
import com.retrocrawler.core.gear.GearResolutionException;
import com.retrocrawler.core.gear.GearTreeFactory;
import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.matcher.GearMatcher;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.core.gear.parser.ParseContext;
import com.retrocrawler.core.progress.ProgressSnapshot;
import com.retrocrawler.core.progress.ProgressStage;
import com.retrocrawler.core.progress.Progressor;

class RetroCrawlerResolutionFailureTest {

	private static final ArchiveId ARCHIVE_ID = ArchiveId.of("resolution_test");

	@TempDir
	private Path archiveRoot;

	@Test
	void failEarlyReportsTheArtifactAndOriginalResolutionFailure() throws IOException {
		Files.createDirectory(archiveRoot.resolve("explosion"));
		final RecordingFactory factory = new RecordingFactory();
		final Journal journal = new Journal();

		final GearResolutionException failure = assertThrows(GearResolutionException.class,
				() -> crawler().crawlAll(journal, ReindexScope.all(), factory));

		assertEquals(ARI.of("resolution_failure_test", ARCHIVE_ID, Path.of("explosion")), failure.source());
		assertInstanceOf(IllegalStateException.class, failure.getCause());
		assertEquals("Parser broke for explosion.", failure.getCause().getMessage());
		assertEquals(List.of(failure), journal.failures());
		assertEquals(0, factory.calls);
	}

	@Test
	void failLateExaminesEveryArtifactAndReportsEveryResolutionFailure() throws IOException {
		final Path duplicate = Files.createDirectory(archiveRoot.resolve("duplicate"));
		Files.createDirectory(duplicate.resolve("explosion"));
		final List<ProgressSnapshot> events = new ArrayList<>();
		final Journal journal = new Journal(Progressor.observing(progress -> events.add(progress.snapshot())),
				FailureMode.FAIL_LATE);
		final RecordingFactory factory = new RecordingFactory();

		final CrawlException report = assertThrows(CrawlException.class,
				() -> crawler().crawlAll(journal, ReindexScope.all(), factory));

		assertEquals(2, report.failures().size());
		final List<GearResolutionException> failures = report.failures().stream()
				.map(GearResolutionException.class::cast).toList();
		assertEquals(
				List.of(ARI.of("resolution_failure_test", ARCHIVE_ID, Path.of("duplicate")),
						ARI.of("resolution_failure_test", ARCHIVE_ID, Path.of("duplicate", "explosion"))),
				failures.stream().map(GearResolutionException::source).toList());
		assertInstanceOf(DuplicateClueException.class, failures.getFirst().getCause());
		assertInstanceOf(IllegalStateException.class, failures.getLast().getCause());
		assertEquals(report.failures(), journal.failures());
		final List<ProgressSnapshot> resolving = events.stream()
				.filter(event -> event.stage().equals(ProgressStage.RESOLVING)).toList();
		assertEquals(2, resolving.getLast().completed());
		assertEquals(2, resolving.getLast().total());
		assertEquals(0, factory.calls);
	}

	@Test
	void failLateContinuesFromClueFindingInOneArchiveToResolutionInAnother() throws IOException {
		final Path failedArchive = Files.createDirectory(archiveRoot.resolve("failed_archive"));
		Files.createDirectory(failedArchive.resolve("clue_failure"));
		final Path resolvableArchive = Files.createDirectory(archiveRoot.resolve("resolvable_archive"));
		Files.createDirectory(resolvableArchive.resolve("explosion"));
		final Model model = Model.from(Set.of(TestArchive.class, TestGear.class));
		final RetroCrawler crawler = RetroCrawler.builder().model(model).repository(new InMemoryRepository())
				.archive(ArchiveDescriptor.of(ArchiveId.of("failed"), failedArchive))
				.archive(ArchiveDescriptor.of(ArchiveId.of("resolvable"), resolvableArchive)).build();
		final Journal journal = new Journal(FailureMode.FAIL_LATE);
		final RecordingFactory factory = new RecordingFactory();

		final CrawlException report = assertThrows(CrawlException.class,
				() -> crawler.crawlAll(journal, ReindexScope.all(), factory));

		assertEquals(2, report.failures().size());
		assertInstanceOf(ClueFindingException.class, report.failures().getFirst());
		final GearResolutionException resolutionFailure = assertInstanceOf(GearResolutionException.class,
				report.failures().getLast());
		assertEquals(ARI.of("resolution_failure_test", ArchiveId.of("resolvable"), Path.of("explosion")),
				resolutionFailure.source());
		assertEquals(0, factory.calls);
	}

	private RetroCrawler crawler() {
		final Model model = Model.from(Set.of(TestArchive.class, TestGear.class));
		return RetroCrawler.builder().model(model).repository(new InMemoryRepository())
				.archive(ArchiveDescriptor.of(ARCHIVE_ID, archiveRoot)).build();
	}

	@RetroCollection(id = "resolution_failure_test")
	@RetroClues(fromFolderName = TestClueFinder.class)
	public static final class TestArchive {

		private TestArchive() {
		}
	}

	public static final class TestClueFinder implements FolderNameClueFinder {

		@Override
		public Clues find(final String folderName) {
			return switch (folderName) {
			case "clue_failure" -> throw new IllegalArgumentException("Clue finder broke.");
			case "duplicate" -> Clues.of(Clue.of("SN"), Clue.of("sn", "12345"));
			case "explosion" -> Clues.of(Clue.of("explode", folderName));
			default -> Clues.none();
			};
		}
	}

	@RetroGear(TestMatcher.class)
	public static final class TestGear {

		@RetroFact(key = "sn")
		private String serialNumber;

		@RetroFact(key = "explode", parser = ExplodingParser.class)
		private String explosion;

		public TestGear() {
		}
	}

	public static final class TestMatcher implements GearMatcher {

		@Override
		public Confidence matches(final com.retrocrawler.core.gear.GearContext context) {
			return Confidence.EXACT;
		}
	}

	public static final class ExplodingParser implements FactParser<String> {

		@Override
		public RatedFact<String> parse(final String rawValue, final ParseContext context) {
			throw new IllegalStateException("Parser broke for " + rawValue + ".");
		}
	}

	private static final class RecordingFactory implements GearTreeFactory<Object, Object, TestGear> {

		private int calls;

		@Override
		public Class<TestGear> gearType() {
			return TestGear.class;
		}

		@Override
		public void beginArchive(final ArchiveDescriptor archive) {
			calls++;
		}

		@Override
		public void endArchive(final ArchiveDescriptor archive) {
			calls++;
		}

		@Override
		public Object addNode(final Object parent, final TestGear gear) {
			calls++;
			return new Object();
		}

		@Override
		public Object build() {
			calls++;
			return new Object();
		}
	}

}
