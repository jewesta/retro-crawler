package com.retrocrawler.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.annotation.RetroAnyAttribute;
import com.retrocrawler.core.annotation.RetroArchive;
import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.PathNameClueFinder;
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
	void createsModelFromTypeSource() {
		final TypeSource source = () -> Set.of(TestArchive.class, TestGear.class);

		final Model model = Model.from(source);

		assertEquals("model_test", model.getArchiveDescriptor().getId().get());
	}

	@Test
	void rejectsMissingArchiveAnnotation() {
		final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
				() -> Model.from(Set.of(TestGear.class)));

		assertEquals("Missing @RetroArchive on provided types.", failure.getMessage());
	}

	@Test
	void rejectsMultipleArchiveAnnotations() {
		final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
				() -> Model.from(Set.of(TestArchive.class, SecondTestArchive.class, TestGear.class)));

		assertTrue(failure.getMessage().contains(TestArchive.class.getName()));
		assertTrue(failure.getMessage().contains(SecondTestArchive.class.getName()));
	}

	@RetroArchive(id = "model_test", locations = "/this/path/is/not-read-during-model-creation",
			findClues = @RetroArchive.LookAt(pathName = EmptyClueFinder.class))
	public static class TestArchive {
	}

	@RetroArchive(id = "second_model_test", locations = "/this/path/is/not-read-during-model-creation",
			findClues = @RetroArchive.LookAt(pathName = EmptyClueFinder.class))
	public static class SecondTestArchive {
	}

	@RetroGear(AnyGearMatcher.class)
	public static class TestGear {

		@RetroAnyAttribute
		private final Map<String, RetroAttribute> attributes = new HashMap<>();

		public TestGear() {
		}
	}

	public static class EmptyClueFinder implements PathNameClueFinder {

		@Override
		public Set<Clue> find(final String pathName) {
			return Set.of();
		}
	}
}
