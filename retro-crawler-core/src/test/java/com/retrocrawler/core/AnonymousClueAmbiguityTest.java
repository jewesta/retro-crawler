package com.retrocrawler.core;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.retrocrawler.core.annotation.RetroAnyAttribute;
import com.retrocrawler.core.annotation.RetroClues;
import com.retrocrawler.core.annotation.RetroCollection;
import com.retrocrawler.core.annotation.RetroFact;
import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.core.archive.ArchiveRoots;
import com.retrocrawler.core.archive.InMemoryRepository;
import com.retrocrawler.core.archive.ReindexScope;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.FolderNameClueFinder;
import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.matcher.AnyGearMatcher;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.core.progress.Progressor;
import com.retrocrawler.core.util.RetroAttribute;

class AnonymousClueAmbiguityTest {

	@TempDir
	private Path archiveRoot;

	@Test
	void retainsAnAnonymousClueWhenEquallyConfidentParsersDisagree() throws IOException {
		Files.createDirectory(archiveRoot.resolve("ambiguous"));
		final Model model = Model.from(Set.of(AmbiguousArchive.class, AmbiguousGear.class),
				ArchiveRoots.from(archiveRoot));
		final RetroCrawler crawler = RetroCrawler.builder().model(model).repository(new InMemoryRepository()).build();

		final List<AmbiguousGear> gear = crawler.crawlGear(new Progressor(), ReindexScope.all(), AmbiguousGear.class);

		assertNull(gear.getFirst().firstMeaning);
		assertNull(gear.getFirst().secondMeaning);
		assertTrue(
				gear.getFirst().attributes.values().stream().map(attribute -> assertInstanceOf(Clue.class, attribute))
						.anyMatch(clue -> clue.isAnonymous() && clue.value().equals(Set.of("overlap"))));
	}

	@RetroCollection(id = "anonymous_ambiguity")
	@RetroClues(fromFolderName = AmbiguousClueFinder.class)
	public static final class AmbiguousArchive {

		private AmbiguousArchive() {
		}
	}

	@RetroGear(AnyGearMatcher.class)
	public static final class AmbiguousGear {

		@RetroFact(key = "first", parser = FirstParser.class, strict = false, optional = true)
		private String firstMeaning;

		@RetroFact(key = "second", parser = SecondParser.class, strict = false, optional = true)
		private String secondMeaning;

		@RetroAnyAttribute
		private final Map<String, RetroAttribute> attributes = new HashMap<>();

		public AmbiguousGear() {
		}
	}

	public static final class AmbiguousClueFinder implements FolderNameClueFinder {

		@Override
		public Set<Clue> find(final String folderName) {
			return "ambiguous".equals(folderName) ? Set.of(Clue.of("overlap")) : Set.of();
		}
	}

	public static final class FirstParser implements FactParser {

		@Override
		public RatedFact parse(final String rawValue) {
			return "overlap".equals(rawValue) ? RatedFact.exact("first") : RatedFact.none("Not the first meaning.");
		}
	}

	public static final class SecondParser implements FactParser {

		@Override
		public RatedFact parse(final String rawValue) {
			return "overlap".equals(rawValue) ? RatedFact.exact("second") : RatedFact.none("Not the second meaning.");
		}
	}
}
