package com.retrocrawler.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.retrocrawler.core.annotation.RetroAnyAttribute;
import com.retrocrawler.core.annotation.RetroAnyGear;
import com.retrocrawler.core.annotation.RetroClues;
import com.retrocrawler.core.annotation.RetroCollection;
import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.archive.InMemoryRepository;
import com.retrocrawler.core.archive.ReindexScope;
import com.retrocrawler.core.archive.clues.ArchiveFolderView;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.ClueFinder;
import com.retrocrawler.core.archive.clues.Clues;
import com.retrocrawler.core.gear.Confidence;
import com.retrocrawler.core.gear.GearContext;
import com.retrocrawler.core.gear.matcher.GearMatcher;
import com.retrocrawler.core.gear.trace.ResolutionTrace;
import com.retrocrawler.core.util.RetroAttribute;

class GearMatchTraceTest {

	@TempDir
	private Path archiveRoot;

	@Test
	void selectsRetroAnyGearAndRecordsAnUnresolvedBestMatchTie() throws IOException {
		Files.createDirectory(archiveRoot.resolve("gear"));
		final Model model = Model.from(Set.of(TestArchive.class, FirstGear.class, SecondGear.class, MysteryGear.class));
		final RetroCrawler crawler = RetroCrawler.builder().model(model).repository(new InMemoryRepository())
				.archive(ArchiveDescriptor.of(ArchiveId.of("test_archive"), archiveRoot)).build();

		final var node = crawler.crawl(new Journal(), ReindexScope.all()).pull().roots().getFirst();
		final ResolutionTrace trace = node.trace().orElseThrow();

		assertInstanceOf(MysteryGear.class, node.gear());
		assertEquals(2, trace.matches().size());
		assertTrue(trace.matches().stream().allMatch(match -> match.confidence() == Confidence.WEAK));
		assertTrue(trace.selectedMatch().isEmpty());
		assertEquals(MysteryGear.class, trace.selection().gearType());
		assertEquals(ResolutionTrace.SelectionKind.FALLBACK_AMBIGUOUS_MATCH, trace.selection().kind());
		assertEquals(1, trace.issues().size());
		assertEquals(ResolutionTrace.Phase.MATCHING, trace.issues().getFirst().phase());
		assertEquals(ResolutionTrace.IssueKind.AMBIGUOUS_GEAR_MATCH, trace.issues().getFirst().kind());
	}

	@Test
	void selectsTheMostSpecificTypeFromAnInheritanceTie() throws IOException {
		Files.createDirectory(archiveRoot.resolve("gear"));
		final Model model = Model.from(Set.of(TestArchive.class, ParentGear.class, ChildGear.class, MysteryGear.class));
		final RetroCrawler crawler = RetroCrawler.builder().model(model).repository(new InMemoryRepository())
				.archive(ArchiveDescriptor.of(ArchiveId.of("test_archive"), archiveRoot)).build();

		final var node = crawler.crawl(new Journal(), ReindexScope.all()).pull().roots().getFirst();
		final ResolutionTrace trace = node.trace().orElseThrow();

		assertInstanceOf(ChildGear.class, node.gear());
		assertEquals(ChildGear.class, trace.selectedMatch().orElseThrow().gearType());
		assertEquals(ResolutionTrace.SelectionKind.MOST_SPECIFIC_MATCH, trace.selection().kind());
		assertTrue(trace.issues().isEmpty());
	}

	@Test
	void confidenceTakesPrecedenceOverTypeSpecificity() throws IOException {
		Files.createDirectory(archiveRoot.resolve("gear"));
		final Model model = Model
				.from(Set.of(TestArchive.class, StrongParentGear.class, WeakChildGear.class, MysteryGear.class));
		final RetroCrawler crawler = RetroCrawler.builder().model(model).repository(new InMemoryRepository())
				.archive(ArchiveDescriptor.of(ArchiveId.of("test_archive"), archiveRoot)).build();

		final var node = crawler.crawl(new Journal(), ReindexScope.all()).pull().roots().getFirst();
		final ResolutionTrace trace = node.trace().orElseThrow();

		assertInstanceOf(StrongParentGear.class, node.gear());
		assertEquals(StrongParentGear.class, trace.selectedMatch().orElseThrow().gearType());
		assertEquals(ResolutionTrace.SelectionKind.MATCH, trace.selection().kind());
	}

	@Test
	void selectsAWeakRealMatchInsteadOfRetroAnyGear() throws IOException {
		Files.createDirectory(archiveRoot.resolve("gear"));
		final Model model = Model.from(Set.of(TestArchive.class, FirstGear.class, MysteryGear.class));
		final RetroCrawler crawler = RetroCrawler.builder().model(model).repository(new InMemoryRepository())
				.archive(ArchiveDescriptor.of(ArchiveId.of("test_archive"), archiveRoot)).build();

		final var node = crawler.crawl(new Journal(), ReindexScope.all()).pull().roots().getFirst();
		final ResolutionTrace trace = node.trace().orElseThrow();

		assertInstanceOf(FirstGear.class, node.gear());
		assertEquals(1, trace.matches().size());
		assertEquals(Confidence.WEAK, trace.selectedMatch().orElseThrow().confidence());
		assertEquals(ResolutionTrace.SelectionKind.MATCH, trace.selection().kind());
		assertTrue(trace.issues().isEmpty());
	}

	@Test
	void selectsRetroAnyGearWhenNoMatcherRecognizesTheArtifact() throws IOException {
		Files.createDirectory(archiveRoot.resolve("gear"));
		final Model model = Model.from(Set.of(TestArchive.class, RejectingGear.class, MysteryGear.class));
		final RetroCrawler crawler = RetroCrawler.builder().model(model).repository(new InMemoryRepository())
				.archive(ArchiveDescriptor.of(ArchiveId.of("test_archive"), archiveRoot)).build();

		final var node = crawler.crawl(new Journal(), ReindexScope.all()).pull().roots().getFirst();
		final ResolutionTrace trace = node.trace().orElseThrow();

		assertInstanceOf(MysteryGear.class, node.gear());
		assertEquals(1, trace.matches().size());
		assertEquals(Confidence.NONE, trace.matches().getFirst().confidence());
		assertTrue(trace.selectedMatch().isEmpty());
		assertEquals(ResolutionTrace.SelectionKind.FALLBACK_NO_MATCH, trace.selection().kind());
		assertTrue(trace.issues().isEmpty());
	}

	@Test
	void producesNoGearForAnUnresolvedTieWithoutRetroAnyGear() throws IOException {
		Files.createDirectory(archiveRoot.resolve("gear"));
		final Model model = Model.from(Set.of(TestArchive.class, FirstGear.class, SecondGear.class));
		final RetroCrawler crawler = RetroCrawler.builder().model(model).repository(new InMemoryRepository())
				.archive(ArchiveDescriptor.of(ArchiveId.of("test_archive"), archiveRoot)).build();

		assertTrue(crawler.crawl(new Journal(), ReindexScope.all()).pull().roots().isEmpty());
	}

	@RetroCollection(id = "gear_match_trace")
	@RetroClues(TestClueFinder.class)
	public static final class TestArchive {
	}

	@RetroGear(WeakMatcher.class)
	public static final class FirstGear extends BaseGear {

		public FirstGear() {
		}
	}

	@RetroGear(WeakMatcher.class)
	public static final class SecondGear extends BaseGear {

		public SecondGear() {
		}
	}

	@RetroGear(WeakMatcher.class)
	public static class ParentGear extends BaseGear {

		public ParentGear() {
		}
	}

	@RetroGear(WeakMatcher.class)
	public static final class ChildGear extends ParentGear {

		public ChildGear() {
		}
	}

	@RetroGear(StrongMatcher.class)
	public static class StrongParentGear extends BaseGear {

		public StrongParentGear() {
		}
	}

	@RetroGear(WeakMatcher.class)
	public static final class WeakChildGear extends StrongParentGear {

		public WeakChildGear() {
		}
	}

	@RetroGear(RejectingMatcher.class)
	public static final class RejectingGear extends BaseGear {

		public RejectingGear() {
		}
	}

	@RetroAnyGear
	public static final class MysteryGear extends BaseGear {

		public MysteryGear() {
		}
	}

	public abstract static class BaseGear {

		@RetroAnyAttribute
		private final Map<String, RetroAttribute> attributes = new HashMap<>();
	}

	public static final class WeakMatcher implements GearMatcher {

		@Override
		public Confidence matches(final GearContext context) {
			return Confidence.WEAK;
		}
	}

	public static final class RejectingMatcher implements GearMatcher {

		@Override
		public Confidence matches(final GearContext context) {
			return Confidence.NONE;
		}
	}

	public static final class StrongMatcher implements GearMatcher {

		@Override
		public Confidence matches(final GearContext context) {
			return Confidence.STRONG;
		}
	}

	public static final class TestClueFinder implements ClueFinder {

		@Override
		public Clues find(final ArchiveFolderView folder) {
			return "gear".equals(folder.name()) ? Clues.of(Clue.of("kind", "gear")) : Clues.none();
		}
	}
}
