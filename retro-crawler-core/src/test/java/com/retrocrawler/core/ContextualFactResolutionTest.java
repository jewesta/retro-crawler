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
import com.retrocrawler.core.annotation.RetroClues;
import com.retrocrawler.core.annotation.RetroCollection;
import com.retrocrawler.core.annotation.RetroFact;
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
import com.retrocrawler.core.gear.Fact;
import com.retrocrawler.core.gear.GearContext;
import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.matcher.AnyGearMatcher;
import com.retrocrawler.core.gear.matcher.GearMatcher;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.core.gear.parser.ParseContext;
import com.retrocrawler.core.util.RetroAttribute;

class ContextualFactResolutionTest {

	private static final ArchiveId ARCHIVE_ID = ArchiveId.of("test_archive");

	@TempDir
	private Path archiveRoot;

	@Test
	void letsTheSelectedGearSpecialistReinterpretAnAnonymousClue() throws IOException {
		Files.createDirectories(archiveRoot.resolve("typed"));
		Files.createDirectories(archiveRoot.resolve("unknown"));

		final Model model = Model.from(Set.of(TestArchive.class, HardDrive.class, Mystery.class));
		final RetroCrawler crawler = RetroCrawler.builder().model(model).repository(new InMemoryRepository())
				.archive(ArchiveDescriptor.of(ARCHIVE_ID, archiveRoot)).build();

		final List<BaseGear> gear = crawler.crawl(new Journal(), ReindexScope.all()).query(BaseGear.class).pull()
				.gear();

		final HardDrive hardDrive = assertInstanceOf(HardDrive.class,
				gear.stream().filter(HardDrive.class::isInstance).findFirst().orElseThrow());
		assertNull(hardDrive.genericMeasurement());
		assertEquals(Set.of("hard-drive-form-factor"), hardDrive.formFactor.value());
		assertTrue(hardDrive.formFactor.source().isAnonymous());
		assertEquals(Set.of("2.5\""), hardDrive.formFactor.source().value());

		final Mystery mystery = assertInstanceOf(Mystery.class,
				gear.stream().filter(Mystery.class::isInstance).findFirst().orElseThrow());
		assertEquals("generic-length", mystery.genericMeasurement());
	}

	@RetroCollection(id = "contextual_fact_resolution")
	@RetroClues(TestClueFinder.class)
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

		@RetroFact(key = "hardDriveFormFactor", parser = HardDriveFormFactorParser.class, strict = false,
				contextual = true)
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
			return context.fact("kind", String.class).filter("hard-drive"::equals).isPresent() ? Confidence.EXACT
					: Confidence.NONE;
		}
	}

	public static final class TestClueFinder implements ClueFinder {

		@Override
		public Clues find(final ArchiveFolderView folder) {
			final String folderName = folder.name();
			if ("typed".equals(folderName)) {
				return Clues.of(Clue.of("HDD"), Clue.of("2.5\""));
			}
			if ("unknown".equals(folderName)) {
				return Clues.of(Clue.of("2.5\""));
			}
			return Clues.none();
		}
	}

	public static final class KindParser implements FactParser<String> {

		@Override
		public RatedFact<String> parse(final String rawValue, final ParseContext context) {
			return "HDD".equals(rawValue) ? RatedFact.exact("hard-drive")
					: RatedFact.none("Expected the hard-drive type marker.");
		}
	}

	public static final class GenericLengthParser implements FactParser<String> {

		@Override
		public RatedFact<String> parse(final String rawValue, final ParseContext context) {
			return "2.5\"".equals(rawValue) ? RatedFact.exact("generic-length")
					: RatedFact.none("Expected a generic length.");
		}
	}

	public static final class HardDriveFormFactorParser implements FactParser<String> {

		@Override
		public RatedFact<String> parse(final String rawValue, final ParseContext context) {
			return "2.5\"".equals(rawValue) ? RatedFact.exact("hard-drive-form-factor")
					: RatedFact.none("Expected a hard-drive form factor.");
		}
	}
}
