package com.retrocrawler.core.gear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.annotation.RetroFact;
import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.core.gear.filter.FilterDefinition;
import com.retrocrawler.core.gear.matcher.GearMatcher;

class GearResolverFactoryFactNameTest {

	@Test
	void usesOneExplicitNameForEveryDeclarationOfAFactKey() {
		final GearResolver resolver = new GearResolverFactory()
				.reflectOn(Set.of(ExplicitNameGear.class, DifferentlyNamedFieldGear.class));

		assertEquals("shared fact", filter(resolver, "shared").name());
	}

	@Test
	void rejectsContradictingExplicitNamesForOneFactKey() {
		assertThrows(IllegalArgumentException.class,
				() -> new GearResolverFactory().reflectOn(Set.of(ExplicitNameGear.class, OtherExplicitNameGear.class)));
	}

	@Test
	void checksEveryDeclarationWhenOneGearContainsTheSameFactKeyTwice() {
		assertThrows(IllegalArgumentException.class,
				() -> new GearResolverFactory().reflectOn(Set.of(ConflictingFieldsGear.class)));
	}

	@Test
	void requiresAnExplicitNameWhenFieldDerivedNamesDisagree() {
		assertThrows(IllegalArgumentException.class, () -> new GearResolverFactory()
				.reflectOn(Set.of(DifferentlyNamedFieldGear.class, AnotherDifferentlyNamedFieldGear.class)));
	}

	private static FilterDefinition<?> filter(final GearResolver resolver, final String key) {
		return resolver.filters().stream().filter(filter -> key.equals(filter.key())).findFirst().orElseThrow();
	}

	@RetroGear(TestMatcher.class)
	public static final class ExplicitNameGear {

		@RetroFact(key = "shared", name = "shared fact")
		private String technicalName;
	}

	@RetroGear(TestMatcher.class)
	public static final class OtherExplicitNameGear {

		@RetroFact(key = "shared", name = "other fact")
		private String technicalName;
	}

	@RetroGear(TestMatcher.class)
	public static final class ConflictingFieldsGear {

		@RetroFact(key = "shared", name = "first name")
		private String first;

		@RetroFact(key = "shared", name = "second name")
		private String second;
	}

	@RetroGear(TestMatcher.class)
	public static final class DifferentlyNamedFieldGear {

		@RetroFact(key = "shared")
		private String marketingName;
	}

	@RetroGear(TestMatcher.class)
	public static final class AnotherDifferentlyNamedFieldGear {

		@RetroFact(key = "shared")
		private String catalogName;
	}

	public static final class TestMatcher implements GearMatcher {

		@Override
		public Confidence matches(final GearContext context) {
			return Confidence.NONE;
		}
	}
}
