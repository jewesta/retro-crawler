package com.retrocrawler.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.retrocrawler.core.discovery.fixture.InitializationProbe;

class ModelTypeDiscoveryTest {

	private static final String FIXTURE_PACKAGE = "com.retrocrawler.core.discovery.fixture";

	@BeforeEach
	void resetInitializationProbe() {
		InitializationProbe.reset();
	}

	@Test
	void discoversAnnotatedTypesRecursivelyBelowBasePackage() {
		final Model model = Model.from(FIXTURE_PACKAGE);

		assertEquals("discovered_model", model.collectionId());
	}

	@Test
	void doesNotInitializeDiscoveredGearClasses() {
		Model.from(FIXTURE_PACKAGE);

		assertFalse(InitializationProbe.wasGearInitialized());
	}

	@Test
	void rejectsBlankBasePackage() {
		final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> Model.from("  "));

		assertEquals("basePackage must not be blank.", failure.getMessage());
	}

	@Test
	void rejectsPackageWithoutModelAnnotations() {
		final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
				() -> Model.from("com.retrocrawler.core.discovery.missing"));

		assertEquals("No types annotated with @RetroCollection, @RetroGear, or @RetroAnyGear found in base package "
				+ "'com.retrocrawler.core.discovery.missing'.", failure.getMessage());
	}
}
