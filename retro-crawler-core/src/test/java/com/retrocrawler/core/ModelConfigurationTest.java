package com.retrocrawler.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
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
import com.retrocrawler.core.archive.clues.ArchiveFolderView;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.ClueFinder;
import com.retrocrawler.core.archive.clues.Clues;
import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.matcher.AnyGearMatcher;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.core.gear.parser.ParseContext;

class ModelConfigurationTest {

	private static final ArchiveId ARCHIVE_ID = ArchiveId.of("test_archive");

	private static final Instant FIXED_INSTANT = Instant.parse("2026-08-04T10:00:00Z");

	@TempDir
	private Path archiveRoot;

	@BeforeEach
	void resetObservedContext() {
		RecordingParser.observedContext = null;
	}

	@Test
	void usesHostFormatLocaleAndTimeZoneByDefault() {
		final Locale expectedLocale = Locale.getDefault(Locale.Category.FORMAT);
		final ZoneId expectedTimeZone = ZoneId.systemDefault();

		final Model model = Model.from(Set.of(DefaultConfigurationCollection.class, ConfiguredGear.class));

		assertEquals(expectedLocale, model.configuration().locale());
		assertEquals(expectedTimeZone, model.configuration().timeZone());
		assertEquals(expectedTimeZone, model.configuration().clock().getZone());
	}

	@Test
	void readsPortableLocaleAndTimeZoneFromRetroCollection() {
		final Model model = Model.from(Set.of(AnnotatedConfigurationCollection.class, ConfiguredGear.class));

		assertEquals(Locale.forLanguageTag("de-DE"), model.configuration().locale());
		assertEquals(ZoneId.of("Europe/Berlin"), model.configuration().timeZone());
		assertEquals(model.configuration().timeZone(), model.configuration().clock().getZone());
	}

	@Test
	void builderOverridesAnnotationConfigurationAndKeepsClockConsistent() {
		final ZoneId runtimeTimeZone = ZoneId.of("America/New_York");
		final Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
		final Model model = Model.builder()
				.typesFrom(Set.of(AnnotatedConfigurationCollection.class, ConfiguredGear.class))
				.configuration(
						config -> config.locale(Locale.CANADA_FRENCH).timeZone(runtimeTimeZone).clock(fixedClock))
				.build();

		assertEquals(Locale.CANADA_FRENCH, model.configuration().locale());
		assertEquals(runtimeTimeZone, model.configuration().timeZone());
		assertEquals(runtimeTimeZone, model.configuration().clock().getZone());
		assertEquals(FIXED_INSTANT, model.configuration().clock().instant());
	}

	@Test
	void explicitBuilderClockCanSupplyTheTimeZone() {
		final Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
		final Model model = Model.builder()
				.typesFrom(Set.of(AnnotatedConfigurationCollection.class, ConfiguredGear.class))
				.configuration(config -> config.clock(fixedClock)).build();

		assertEquals(ZoneOffset.UTC, model.configuration().timeZone());
		assertEquals(fixedClock, model.configuration().clock());
	}

	@Test
	void rejectsInvalidAnnotationLocale() {
		final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
				() -> Model.from(Set.of(InvalidLocaleCollection.class, ConfiguredGear.class)));

		assertTrue(failure.getMessage().contains("Invalid locale on @RetroCollection"));
	}

	@Test
	void rejectsInvalidAnnotationTimeZone() {
		final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
				() -> Model.from(Set.of(InvalidTimeZoneCollection.class, ConfiguredGear.class)));

		assertTrue(failure.getMessage().contains("Invalid timeZone on @RetroCollection"));
	}

	@Test
	void rejectsDuplicateBuilderConfiguration() {
		final Model.Builder builder = Model.builder()
				.typesFrom(Set.of(DefaultConfigurationCollection.class, ConfiguredGear.class))
				.configuration(config -> config.locale(Locale.GERMANY));

		final IllegalStateException failure = assertThrows(IllegalStateException.class,
				() -> builder.configuration(config -> config.locale(Locale.FRANCE)));

		assertEquals("Configuration is already configured.", failure.getMessage());
	}

	@Test
	void passesEffectiveConfigurationAndArtifactAriToParsers() throws IOException {
		Files.createDirectory(archiveRoot.resolve("gear"));
		final Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZoneId.of("Europe/Berlin"));
		final Model model = Model.builder().typesFrom(Set.of(RuntimeContextCollection.class, RuntimeContextGear.class))
				.configuration(config -> config.locale(Locale.GERMANY).clock(fixedClock)).build();
		final RetroCrawler crawler = RetroCrawler.builder().model(model).repository(new InMemoryRepository())
				.archive(ArchiveDescriptor.of(ARCHIVE_ID, archiveRoot)).build();

		final List<RuntimeContextGear> gear = crawler.crawlAllGear(new Journal(), ReindexScope.all(),
				RuntimeContextGear.class);

		assertEquals(1, gear.size());
		final ParseContext context = RecordingParser.observedContext;
		assertSame(model.configuration(), context.config());
		assertEquals(ARI.of("runtime_context", ARCHIVE_ID, Path.of("gear")), context.source());
	}

	@RetroCollection(id = "default_configuration")
	@RetroClues(EmptyClueFinder.class)
	public static final class DefaultConfigurationCollection {
	}

	@RetroCollection(id = "annotated_configuration", locale = "de-DE", timeZone = "Europe/Berlin")
	@RetroClues(EmptyClueFinder.class)
	public static final class AnnotatedConfigurationCollection {
	}

	@RetroCollection(id = "invalid_locale", locale = "de_DE")
	@RetroClues(EmptyClueFinder.class)
	public static final class InvalidLocaleCollection {
	}

	@RetroCollection(id = "invalid_time_zone", timeZone = "Mars/Olympus")
	@RetroClues(EmptyClueFinder.class)
	public static final class InvalidTimeZoneCollection {
	}

	@RetroGear(AnyGearMatcher.class)
	public static final class ConfiguredGear {

		@RetroFact
		private String value;
	}

	@RetroCollection(id = "runtime_context")
	@RetroClues(RuntimeContextClueFinder.class)
	public static final class RuntimeContextCollection {
	}

	@RetroGear(AnyGearMatcher.class)
	public static final class RuntimeContextGear {

		@RetroFact(key = "observed", parser = RecordingParser.class)
		private String observed;
	}

	public static final class EmptyClueFinder implements ClueFinder {

		@Override
		public Clues find(final ArchiveFolderView folder) {
			return Clues.none();
		}
	}

	public static final class RuntimeContextClueFinder implements ClueFinder {

		@Override
		public Clues find(final ArchiveFolderView folder) {
			final String folderName = folder.name();
			return "gear".equals(folderName) ? Clues.of(Clue.of("observed", "value")) : Clues.none();
		}
	}

	public static final class RecordingParser implements FactParser<String> {

		private static ParseContext observedContext;

		@Override
		public RatedFact<String> parse(final String rawValue, final ParseContext context) {
			observedContext = context;
			return RatedFact.exact(rawValue);
		}
	}
}
