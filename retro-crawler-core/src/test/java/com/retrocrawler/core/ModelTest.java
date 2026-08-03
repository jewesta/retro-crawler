package com.retrocrawler.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.annotation.RetroAnyAttribute;
import com.retrocrawler.core.annotation.RetroClues;
import com.retrocrawler.core.annotation.RetroCollection;
import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.core.archive.ArchiveRoots;
import com.retrocrawler.core.archive.CrawlEverything;
import com.retrocrawler.core.archive.CrawlPolicy;
import com.retrocrawler.core.archive.IgnoreSystemFiles;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.FolderNameClueFinder;
import com.retrocrawler.core.gear.TypeSource;
import com.retrocrawler.core.gear.matcher.AnyGearMatcher;
import com.retrocrawler.core.util.RetroAttribute;

class ModelTest {

	@Test
	void createsModelFromExplicitTypes() {
		final Model model = Model.from(Set.of(TestArchive.class, TestGear.class));

		assertEquals("model_test", model.getArchiveDescriptor().getId().get());
	}

	@Test
	void createsAnnotationConfiguredCrawlPolicy() {
		final Model model = Model.from(Set.of(FilteredArchive.class, TestGear.class));

		assertInstanceOf(IgnoreSystemFiles.class, model.crawlPolicy());
	}

	@Test
	void preservesIncludeEverythingAsTheDefaultCrawlPolicy() {
		final Model model = Model.from(Set.of(TestArchive.class, TestGear.class));

		assertInstanceOf(CrawlEverything.class, model.crawlPolicy());
	}

	@Test
	void builderOverridesAnnotationCrawlPolicy() {
		final CrawlPolicy runtimePolicy = path -> false;
		final Model model = Model.builder().typesFrom(Set.of(FilteredArchive.class, TestGear.class))
				.crawlPolicy(runtimePolicy).build();

		assertSame(runtimePolicy, model.crawlPolicy());
	}

	@Test
	void createsModelFromTypeSource() {
		final TypeSource source = () -> Set.of(TestArchive.class, TestGear.class);

		final Model model = Model.from(source);

		assertEquals("model_test", model.getArchiveDescriptor().getId().get());
	}

	@Test
	void overridesAnnotationLocationsAtRuntime() {
		final Path runtimeLocation = Path.of("/runtime/archive");

		final Model model = Model.from(Set.of(TestArchive.class, TestGear.class), ArchiveRoots.from(runtimeLocation));

		assertEquals(List.of(runtimeLocation), model.getArchiveDescriptor().getPaths());
	}

	@Test
	void builderOverridesAnnotationLocationsAndWorkingDirectory() {
		final Path runtimeLocation = Path.of("/runtime/collection");
		final Path runtimeWorkingDirectory = Path.of("/runtime/work");

		final Model model = Model.builder().typesFrom(Set.of(WorkingDirectoryCollection.class, TestGear.class))
				.locations(runtimeLocation).workingDirectory(runtimeWorkingDirectory).build();

		assertEquals(List.of(runtimeLocation), model.getArchiveDescriptor().getPaths());
		assertEquals(runtimeWorkingDirectory, model.getWorkingDirectory().orElseThrow());
	}

	@Test
	void usesAnnotationWorkingDirectoryAsPortableDefault() {
		final Model model = Model.from(Set.of(WorkingDirectoryCollection.class, TestGear.class));

		assertEquals(Path.of("annotation-work"), model.getWorkingDirectory().orElseThrow());
	}

	@Test
	void permitsAnnotationLocationsToBeSuppliedAtRuntime() {
		final Path runtimeLocation = Path.of("/runtime/archive");

		final Model model = Model.from(Set.of(RuntimeConfiguredArchive.class, TestGear.class),
				ArchiveRoots.from(runtimeLocation));

		assertEquals(List.of(runtimeLocation), model.getArchiveDescriptor().getPaths());
	}

	@Test
	void copiesRuntimeLocations() {
		final List<Path> runtimeLocations = new ArrayList<>(List.of(Path.of("/runtime/archive")));
		final ArchiveRoots archiveRoots = ArchiveRoots.from(runtimeLocations);
		final Model model = Model.from(Set.of(RuntimeConfiguredArchive.class, TestGear.class), archiveRoots);

		runtimeLocations.add(Path.of("/another/archive"));

		assertEquals(List.of(Path.of("/runtime/archive")), model.getArchiveDescriptor().getPaths());
	}

	@Test
	void rejectsMissingAnnotationAndRuntimeLocations() {
		final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
				() -> Model.from(Set.of(RuntimeConfiguredArchive.class, TestGear.class)));

		assertEquals("A @RetroCollection requires at least one location to be set.", failure.getMessage());
	}

	@Test
	void rejectsEmptyRuntimeLocations() {
		final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
				() -> Model.from(Set.of(TestArchive.class, TestGear.class), () -> List.of()));

		assertEquals("A @RetroCollection requires at least one location to be set.", failure.getMessage());
	}

	@Test
	void rejectsMissingCollectionAnnotation() {
		final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
				() -> Model.from(Set.of(TestGear.class)));

		assertEquals("Missing @RetroCollection on provided types.", failure.getMessage());
	}

	@Test
	void rejectsMultipleCollectionAnnotations() {
		final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
				() -> Model.from(Set.of(TestArchive.class, SecondTestArchive.class, TestGear.class)));

		assertTrue(failure.getMessage().contains(TestArchive.class.getName()));
		assertTrue(failure.getMessage().contains(SecondTestArchive.class.getName()));
	}

	@Test
	void requiresClueConfigurationOnTheCollectionType() {
		final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
				() -> Model.from(Set.of(CollectionWithoutClues.class, TestGear.class)));

		assertTrue(failure.getMessage().contains("Missing @RetroClues on @RetroCollection"));
	}

	@RetroCollection(id = "model_test", locations = "/this/path/is/not-read-during-model-creation")
	@RetroClues(fromFolderName = EmptyClueFinder.class)
	public static class TestArchive {
	}

	@RetroCollection(id = "second_model_test", locations = "/this/path/is/not-read-during-model-creation")
	@RetroClues(fromFolderName = EmptyClueFinder.class)
	public static class SecondTestArchive {
	}

	@RetroCollection(id = "runtime_model_test")
	@RetroClues(fromFolderName = EmptyClueFinder.class)
	public static class RuntimeConfiguredArchive {
	}

	@RetroCollection(id = "filtered_archive", locations = "/not/read",
			crawlPolicy = IgnoreSystemFiles.class)
	@RetroClues(fromFolderName = EmptyClueFinder.class)
	public static class FilteredArchive {
	}

	@RetroCollection(id = "working_directory", locations = "/not/read",
			workingDirectory = "annotation-work")
	@RetroClues(fromFolderName = EmptyClueFinder.class)
	public static class WorkingDirectoryCollection {
	}

	@RetroCollection(id = "without_clues", locations = "/not/read")
	public static class CollectionWithoutClues {
	}

	@RetroGear(AnyGearMatcher.class)
	public static class TestGear {

		@RetroAnyAttribute
		private final Map<String, RetroAttribute> attributes = new HashMap<>();

		public TestGear() {
		}
	}

	public static class EmptyClueFinder implements FolderNameClueFinder {

		@Override
		public Set<Clue> find(final String folderName) {
			return Set.of();
		}
	}
}
