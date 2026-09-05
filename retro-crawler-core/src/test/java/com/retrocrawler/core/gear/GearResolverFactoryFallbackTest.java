package com.retrocrawler.core.gear;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.annotation.RetroAnyAttribute;
import com.retrocrawler.core.annotation.RetroAnyGear;
import com.retrocrawler.core.util.RetroAttribute;

class GearResolverFactoryFallbackTest {

	@Test
	void rejectsMoreThanOneRetroAnyGearType() {
		final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
				() -> new GearResolverFactory().reflectOn(Set.of(FirstFallback.class, SecondFallback.class)));

		assertTrue(failure.getMessage().contains(RetroAnyGear.class.getSimpleName()));
		assertTrue(failure.getMessage().contains(FirstFallback.class.getName()));
		assertTrue(failure.getMessage().contains(SecondFallback.class.getName()));
	}

	@RetroAnyGear
	public static final class FirstFallback extends BaseGear {
	}

	@RetroAnyGear
	public static final class SecondFallback extends BaseGear {
	}

	public abstract static class BaseGear {

		@RetroAnyAttribute
		private final Map<String, RetroAttribute> attributes = new HashMap<>();
	}
}
