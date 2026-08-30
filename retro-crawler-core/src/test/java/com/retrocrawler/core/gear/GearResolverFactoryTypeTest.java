package com.retrocrawler.core.gear;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.annotation.RetroAnyAttribute;
import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.core.gear.matcher.GearMatcher;
import com.retrocrawler.core.util.RetroAttribute;

class GearResolverFactoryTypeTest {

	@Test
	void rejectsCollidingDerivedGearTypeKeys() {
		final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
				() -> new GearResolverFactory().reflectOn(Set.of(First.Duplicate.class, Second.Duplicate.class)));

		assertTrue(failure.getMessage().contains("Gear type key 'duplicate'"));
		assertTrue(failure.getMessage().contains(First.Duplicate.class.getName()));
		assertTrue(failure.getMessage().contains(Second.Duplicate.class.getName()));
	}

	@Test
	void acceptsAnExplicitKeyResolvingAClassNameCollision() {
		assertDoesNotThrow(
				() -> new GearResolverFactory().reflectOn(Set.of(First.Duplicate.class, Explicit.Duplicate.class)));
	}

	private abstract static class BaseGear {

		@RetroAnyAttribute
		private final Map<String, RetroAttribute> attributes = new HashMap<>();
	}

	private static final class First {

		@RetroGear(TestMatcher.class)
		public static final class Duplicate extends BaseGear {
		}
	}

	private static final class Second {

		@RetroGear(TestMatcher.class)
		public static final class Duplicate extends BaseGear {
		}
	}

	private static final class Explicit {

		@RetroGear(value = TestMatcher.class, key = "explicit-duplicate")
		public static final class Duplicate extends BaseGear {
		}
	}

	public static final class TestMatcher implements GearMatcher {

		@Override
		public Confidence matches(final GearContext context) {
			return Confidence.NONE;
		}
	}
}
