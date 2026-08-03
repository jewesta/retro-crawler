package com.retrocrawler.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.retrocrawler.core.annotation.RetroAnyAttribute;
import com.retrocrawler.core.annotation.RetroClues;
import com.retrocrawler.core.annotation.RetroCollection;
import com.retrocrawler.core.annotation.RetroFact;
import com.retrocrawler.core.annotation.RetroFactCatalog;
import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.FolderNameClueFinder;
import com.retrocrawler.core.catalog.CatalogLoader;
import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.matcher.AnyGearMatcher;
import com.retrocrawler.core.gear.parser.AbstractCatalogFactParser;
import com.retrocrawler.core.util.RetroAttribute;

class ModelCatalogConfigurationTest {

	@TempDir
	Path temporaryDirectory;

	@BeforeEach
	void resetParserObservation() {
		TestCatalogParser.loadedValue = null;
	}

	@Test
	void loadsAnAnnotationConfiguredCatalogForAUsedParser() throws IOException {
		writeCatalog("annotation.tsv", "from annotation");

		Model.builder().typesFrom(Set.of(AnnotatedCollection.class, CatalogGear.class))
				.workingDirectory(temporaryDirectory).build();

		assertEquals("from annotation", TestCatalogParser.loadedValue);
	}

	@Test
	void builderCatalogConfigurationOverridesTheAnnotation() throws IOException {
		writeCatalog("annotation.tsv", "from annotation");
		writeCatalog("builder.tsv", "from builder");

		Model.builder().typesFrom(Set.of(AnnotatedCollection.class, CatalogGear.class))
				.workingDirectory(temporaryDirectory)
				.factCatalog(TestCatalogParser.class, configuration -> configuration.catalogFile("builder.tsv"))
				.build();

		assertEquals("from builder", TestCatalogParser.loadedValue);
	}

	@Test
	void usesAUserParserDefaultBelowTheWorkingCatalogDirectory() throws IOException {
		writeCatalog("default.tsv", "from parser default");

		Model.builder().typesFrom(Set.of(DefaultConfiguredCollection.class, CatalogGear.class))
				.workingDirectory(temporaryDirectory).build();

		assertEquals("from parser default", TestCatalogParser.loadedValue);
	}

	@Test
	void unusedParserConfigurationDoesNotLoadOrAccessItsCatalog() {
		Model.builder().typesFrom(Set.of(AnnotatedCollection.class, GearWithoutFacts.class))
				.workingDirectory(temporaryDirectory).build();

		assertNull(TestCatalogParser.loadedValue);
	}

	@Test
	void requiresAWorkingDirectoryOnlyWhenAnExternalCatalogIsUsed() {
		final IllegalStateException failure = assertThrows(IllegalStateException.class,
				() -> Model.from(Set.of(AnnotatedCollection.class, CatalogGear.class)));

		assertTrue(failure.getMessage().contains("requires a collection working directory"));
	}

	@Test
	void rejectsCatalogPathsThatEscapeTheCatalogDirectory() {
		final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> Model.builder()
				.typesFrom(Set.of(AnnotatedCollection.class, CatalogGear.class)).workingDirectory(temporaryDirectory)
				.factCatalog(TestCatalogParser.class, configuration -> configuration.catalogFile("../outside.tsv"))
				.build());

		assertTrue(failure.getMessage().contains("relative path"));
	}

	private void writeCatalog(final String fileName, final String value) throws IOException {
		final Path catalogs = temporaryDirectory.resolve("catalogs");
		Files.createDirectories(catalogs);
		Files.writeString(catalogs.resolve(fileName), "value\n" + value + "\n", StandardCharsets.UTF_8);
	}

	private enum Key {
		value
	}

	public static final class TestCatalogParser extends AbstractCatalogFactParser<Key> {

		private static String loadedValue;

		public TestCatalogParser(final CatalogLoader catalogs) {
			super(catalogs, Key.class, "default.tsv");
			loadedValue = catalog().rows().getFirst().get(Key.value);
		}

		@Override
		public RatedFact parse(final String rawValue) {
			return RatedFact.exact(rawValue);
		}
	}

	@RetroCollection(id = "catalog_configuration", locations = "/not/read")
	@RetroClues(fromFolderName = EmptyClueFinder.class)
	@RetroFactCatalog(parser = TestCatalogParser.class, catalogFile = "annotation.tsv")
	public static final class AnnotatedCollection {
	}

	@RetroCollection(id = "default_catalog_configuration", locations = "/not/read")
	@RetroClues(fromFolderName = EmptyClueFinder.class)
	public static final class DefaultConfiguredCollection {
	}

	@RetroGear(AnyGearMatcher.class)
	public static final class CatalogGear {

		@RetroFact(parser = TestCatalogParser.class)
		private String value;

		public CatalogGear() {
		}
	}

	@RetroGear(AnyGearMatcher.class)
	public static final class GearWithoutFacts {

		@RetroAnyAttribute
		private final Map<String, RetroAttribute> attributes = new HashMap<>();

		public GearWithoutFacts() {
		}
	}

	public static final class EmptyClueFinder implements FolderNameClueFinder {

		@Override
		public Set<Clue> find(final String folderName) {
			return Set.of();
		}
	}
}
