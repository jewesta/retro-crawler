package com.retrocrawler.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.retrocrawler.core.gear.matcher.AnyGearMatcher;
import com.retrocrawler.core.gear.trace.ResolutionTrace;
import com.retrocrawler.core.util.RetroAttribute;

class GearMatchTraceTest {

	@TempDir
	private Path archiveRoot;

	@Test
	void recordsEveryMatcherAndAnEqualBestMatchAmbiguity() throws IOException {
		Files.createDirectory(archiveRoot.resolve("gear"));
		final Model model = Model.from(Set.of(TestArchive.class, FirstGear.class, SecondGear.class));
		final RetroCrawler crawler = RetroCrawler.builder().model(model).repository(new InMemoryRepository())
				.archive(ArchiveDescriptor.of(ArchiveId.of("test_archive"), archiveRoot)).build();

		final ResolutionTrace trace = crawler.crawl(new Journal(), ReindexScope.all()).pull().roots().getFirst().trace()
				.orElseThrow();

		assertEquals(2, trace.matches().size());
		assertTrue(trace.matches().stream().allMatch(match -> match.confidence() == Confidence.WEAK));
		assertTrue(trace.matches().contains(trace.selectedMatch()));
		assertEquals(1, trace.issues().size());
		assertEquals(ResolutionTrace.Phase.MATCHING, trace.issues().getFirst().phase());
		assertEquals(ResolutionTrace.IssueKind.AMBIGUOUS_GEAR_MATCH, trace.issues().getFirst().kind());
	}

	@RetroCollection(id = "gear_match_trace")
	@RetroClues(TestClueFinder.class)
	public static final class TestArchive {
	}

	@RetroGear(AnyGearMatcher.class)
	public static final class FirstGear extends BaseGear {

		public FirstGear() {
		}
	}

	@RetroGear(AnyGearMatcher.class)
	public static final class SecondGear extends BaseGear {

		public SecondGear() {
		}
	}

	public abstract static class BaseGear {

		@RetroAnyAttribute
		private final Map<String, RetroAttribute> attributes = new HashMap<>();
	}

	public static final class TestClueFinder implements ClueFinder {

		@Override
		public Clues find(final ArchiveFolderView folder) {
			return "gear".equals(folder.name()) ? Clues.of(Clue.of("kind", "gear")) : Clues.none();
		}
	}
}
