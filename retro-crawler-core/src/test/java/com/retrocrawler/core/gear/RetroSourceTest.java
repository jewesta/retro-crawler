package com.retrocrawler.core.gear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.Configuration;
import com.retrocrawler.core.annotation.RetroFact;
import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.core.annotation.RetroSource;
import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.archive.clues.Artifact;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.Clues;
import com.retrocrawler.core.gear.matcher.AnyGearMatcher;
import com.retrocrawler.core.gear.parser.ParseContext;

class RetroSourceTest {

	private static final ARI SOURCE = ARI.of("test", ArchiveId.of("archive"), Path.of("gear"));
	private static final Artifact ARTIFACT = new Artifact(Clues.of(Clue.of("name", "gear")));
	private static final ParseContext CONTEXT = new ParseContext(Configuration.builder().build(), SOURCE);

	@Test
	void injectsTheRequiredResolutionSourceWhenRequestedByGear() {
		final GearResolver resolver = new GearResolverFactory().reflectOn(Set.of(SourceGear.class));

		final SourceGear gear = (SourceGear) resolver.resolve(ARTIFACT, CONTEXT).orElseThrow();

		assertEquals(SOURCE, gear.source);
	}

	@Test
	void leavesGearWithoutRetroSourceUnchanged() {
		final GearResolver resolver = new GearResolverFactory().reflectOn(Set.of(GearWithoutSource.class));

		final GearWithoutSource gear = (GearWithoutSource) resolver.resolve(ARTIFACT, CONTEXT).orElseThrow();

		assertEquals("gear", gear.name);
	}

	@Test
	void requiresAriForEveryParseContext() {
		assertThrows(NullPointerException.class, () -> new ParseContext(Configuration.builder().build(), null));
	}

	@Test
	void rejectsRetroSourceOnAnyTypeOtherThanAri() {
		final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
				() -> GearDescriptor.of(WrongSourceTypeGear.class));

		assertTrue(failure.getMessage().contains(RetroSource.class.getSimpleName()));
		assertTrue(failure.getMessage().contains(ARI.class.getSimpleName()));
	}

	@Test
	void rejectsMoreThanOneRetroSourceInATypeHierarchy() {
		final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
				() -> GearDescriptor.of(DuplicateSourceGear.class));

		assertTrue(failure.getMessage().contains("Duplicate"));
		assertTrue(failure.getMessage().contains(RetroSource.class.getSimpleName()));
	}

	@Test
	void rejectsRetroSourceOnAStaticField() {
		final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
				() -> GearDescriptor.of(StaticSourceGear.class));

		assertTrue(failure.getMessage().contains(RetroSource.class.getSimpleName()));
		assertTrue(failure.getMessage().contains("static"));
	}

	@Test
	void rejectsUsingRetroSourceAsAFact() {
		final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
				() -> GearDescriptor.of(SourceFactGear.class));

		assertTrue(failure.getMessage().contains(RetroSource.class.getSimpleName()));
		assertTrue(failure.getMessage().contains(RetroFact.class.getSimpleName()));
	}

	@RetroGear(AnyGearMatcher.class)
	public static final class SourceGear {

		@RetroSource
		private ARI source;

		@RetroFact
		private String name;
	}

	@RetroGear(AnyGearMatcher.class)
	public static final class GearWithoutSource {

		@RetroFact
		private String name;
	}

	@RetroGear(AnyGearMatcher.class)
	public static final class WrongSourceTypeGear {

		@RetroSource
		private String source;

		@RetroFact
		private String name;
	}

	public static class SourceGearBase {

		@RetroSource
		private ARI inheritedSource;
	}

	@RetroGear(AnyGearMatcher.class)
	public static final class DuplicateSourceGear extends SourceGearBase {

		@RetroSource
		private ARI source;

		@RetroFact
		private String name;
	}

	@RetroGear(AnyGearMatcher.class)
	public static final class SourceFactGear {

		@RetroSource
		@RetroFact
		private ARI source;
	}

	@RetroGear(AnyGearMatcher.class)
	public static final class StaticSourceGear {

		@RetroSource
		private static ARI source;

		@RetroFact
		private String name;
	}
}
