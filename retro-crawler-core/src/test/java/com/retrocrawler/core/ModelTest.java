package com.retrocrawler.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.annotation.RetroAnyAttribute;
import com.retrocrawler.core.annotation.RetroClues;
import com.retrocrawler.core.annotation.RetroCollection;
import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.Clues;
import com.retrocrawler.core.archive.clues.FolderNameClueFinder;
import com.retrocrawler.core.archive.filter.ArchivePathFilter;
import com.retrocrawler.core.archive.filter.IgnoreDotPaths;
import com.retrocrawler.core.archive.filter.IgnoreWindowsSystemPaths;
import com.retrocrawler.core.gear.TypeSource;
import com.retrocrawler.core.gear.matcher.AnyGearMatcher;
import com.retrocrawler.core.util.RetroAttribute;

class ModelTest {

	@Test
	void createsModelFromExplicitTypes() {
		final Model model = Model.from(Set.of(TestArchive.class, TestGear.class));

		assertEquals("model_test", model.collectionId());
	}

	@Test
	void usesTheCollectionIdAsItsDefaultName() {
		final Model model = Model.from(Set.of(TestArchive.class, TestGear.class));

		assertEquals("model_test", model.collectionName());
	}

	@Test
	void createsAnnotationConfiguredArchivePathFiltersInDeclaredOrder() {
		final Model model = Model.from(Set.of(FilteredArchive.class, TestGear.class));

		assertEquals(List.of(IgnoreDotPaths.class, IgnoreWindowsSystemPaths.class),
				model.pathFilters().stream().map(Object::getClass).toList());
	}

	@Test
	void hasNoArchivePathFiltersByDefault() {
		final Model model = Model.from(Set.of(TestArchive.class, TestGear.class));

		assertEquals(List.of(), model.pathFilters());
	}

	@Test
	void builderOverridesAnnotationArchivePathFilters() {
		final ArchivePathFilter first = path -> false;
		final ArchivePathFilter second = path -> true;
		final Model model = Model.builder().typesFrom(Set.of(FilteredArchive.class, TestGear.class))
				.pathFilters(first, second).build();

		assertEquals(List.of(first, second), model.pathFilters());
	}

	@Test
	void createsModelFromTypeSource() {
		final TypeSource source = () -> Set.of(TestArchive.class, TestGear.class);

		final Model model = Model.from(source);

		assertEquals("model_test", model.collectionId());
	}

	@Test
	void builderOverridesAnnotationWorkingDirectory() {
		final Path runtimeWorkingDirectory = Path.of("/runtime/work");

		final Model model = Model.builder().typesFrom(Set.of(WorkingDirectoryCollection.class, TestGear.class))
				.workingDirectory(runtimeWorkingDirectory).build();

		assertEquals(runtimeWorkingDirectory, model.workingDirectory().orElseThrow());
	}

	@Test
	void usesAnnotationWorkingDirectoryAsPortableDefault() {
		final Model model = Model.from(Set.of(WorkingDirectoryCollection.class, TestGear.class));

		assertEquals(Path.of("annotation-work"), model.workingDirectory().orElseThrow());
	}

	@Test
	void createsAModelWithoutAnyArchiveConfiguration() {
		final Model model = Model.from(Set.of(RuntimeConfiguredArchive.class, TestGear.class));

		assertEquals("runtime_model_test", model.collectionId());
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

	@RetroCollection(id = "model_test")
	@RetroClues(fromFolderName = EmptyClueFinder.class)
	public static class TestArchive {
	}

	@RetroCollection(id = "second_model_test")
	@RetroClues(fromFolderName = EmptyClueFinder.class)
	public static class SecondTestArchive {
	}

	@RetroCollection(id = "runtime_model_test")
	@RetroClues(fromFolderName = EmptyClueFinder.class)
	public static class RuntimeConfiguredArchive {
	}

	@RetroCollection(id = "filtered_archive", pathFilters = {
			IgnoreDotPaths.class, IgnoreWindowsSystemPaths.class
	})
	@RetroClues(fromFolderName = EmptyClueFinder.class)
	public static class FilteredArchive {
	}

	@RetroCollection(id = "working_directory", workingDirectory = "annotation-work")
	@RetroClues(fromFolderName = EmptyClueFinder.class)
	public static class WorkingDirectoryCollection {
	}

	@RetroCollection(id = "without_clues")
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
		public Clues find(final String folderName) {
			return Clues.none();
		}
	}
}
