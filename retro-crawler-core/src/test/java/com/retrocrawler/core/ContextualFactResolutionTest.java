package com.retrocrawler.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.retrocrawler.core.annotation.RetroArchive;
import com.retrocrawler.core.annotation.RetroFact;
import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.core.archive.ArchiveRoots;
import com.retrocrawler.core.archive.InMemoryRepository;
import com.retrocrawler.core.archive.ReindexScope;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.Confidence;
import com.retrocrawler.core.archive.clues.PathNameClueFinder;
import com.retrocrawler.core.gear.Fact;
import com.retrocrawler.core.gear.GearContext;
import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.matcher.AnyGearMatcher;
import com.retrocrawler.core.gear.matcher.GearMatcher;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.core.progress.Progressor;
import com.retrocrawler.core.util.RetroAttribute;

class ContextualFactResolutionTest {

	@TempDir
	private Path archiveRoot;

	@Test
	void letsTheSelectedGearSpecialistReinterpretAnAnonymousClue() throws IOException {
		Files.createDirectories(archiveRoot.resolve("typed"));
		Files.createDirectories(archiveRoot.resolve("unknown"));

		final Model model = Model.from(Set.of(TestArchive.class, HardDrive.class, Mystery.class),
				ArchiveRoots.from(archiveRoot));
		final RetroCrawler crawler = RetroCrawler.builder().model(model)
				.repository(new InMemoryRepository()).build();

		final List<BaseGear> gear = crawler.crawlGear(new Progressor(), ReindexScope.all(), BaseGear.class);

		final HardDrive hardDrive = assertInstanceOf(HardDrive.class,
				gear.stream().filter(HardDrive.class::isInstance).findFirst().orElseThrow());
		assertNull(hardDrive.genericMeasurement());
		assertEquals(Set.of("hard-drive-form-factor"), hardDrive.formFactor.getValue());
		assertTrue(hardDrive.formFactor.source().isAnonymous());
		assertEquals(Set.of("2.5\""), hardDrive.formFactor.source().getValue());

		final Mystery mystery = assertInstanceOf(Mystery.class,
				gear.stream().filter(Mystery.class::isInstance).findFirst().orElseThrow());
		assertEquals("generic-length", mystery.genericMeasurement());
	}

	@RetroArchive(id = "contextual_fact_resolution",
			findClues = @RetroArchive.LookAt(pathName = TestClueFinder.class))
	public static final class TestArchive {
	}

	public abstract static class BaseGear {

		@RetroFact(key = "kind", parser = KindParser.class, strict = false)
		private String kind;

		@RetroFact(key = "length", parser = GenericLengthParser.class, strict = false)
		private String genericMeasurement;

		@RetroAnyAttribute
		private final Map<String, RetroAttribute> attributes = new HashMap<>();

		String genericMeasurement() {
			return genericMeasurement;
		}
	}

	@RetroGear(HardDriveMatcher.class)
	public static final class HardDrive extends BaseGear {

		@RetroFact(key = "hardDriveFormFactor", parser = HardDriveFormFactorParser.class,
				strict = false, contextual = true)
		private Fact formFactor;

		public HardDrive() {
		}
	}

	@RetroGear(AnyGearMatcher.class)
	public static final class Mystery extends BaseGear {

		public Mystery() {
		}
	}

	public static final class HardDriveMatcher implements GearMatcher {

		@Override
		public Confidence matches(final GearContext context) {
			return context.getFact("kind", String.class).filter("hard-drive"::equals).isPresent()
					? Confidence.EXACT
					: Confidence.NONE;
		}
	}

	public static final class TestClueFinder implements PathNameClueFinder {

		@Override
		public Set<Clue> find(final String pathName) {
			if ("typed".equals(pathName)) {
				return Set.of(Clue.of("HDD"), Clue.of("2.5\""));
			}
			if ("unknown".equals(pathName)) {
				return Set.of(Clue.of("2.5\""));
			}
			return Set.of();
		}
	}

	public static final class KindParser implements FactParser {

		@Override
		public RatedFact parse(final String rawValue) {
			return "HDD".equals(rawValue) ? RatedFact.exact("hard-drive")
					: RatedFact.none("Expected the hard-drive type marker.");
		}
	}

	public static final class GenericLengthParser implements FactParser {

		@Override
		public RatedFact parse(final String rawValue) {
			return "2.5\"".equals(rawValue) ? RatedFact.exact("generic-length")
					: RatedFact.none("Expected a generic length.");
		}
	}

	public static final class HardDriveFormFactorParser implements FactParser {

		@Override
		public RatedFact parse(final String rawValue) {
			return "2.5\"".equals(rawValue) ? RatedFact.exact("hard-drive-form-factor")
					: RatedFact.none("Expected a hard-drive form factor.");
		}
	}
}
