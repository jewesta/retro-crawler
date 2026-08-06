package com.retrocrawler.core.gear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RatedFactTest {

	private enum Bus {
		PCI
	}

	@Test
	void retainsASingleTypedValue() {
		final RatedFact<Bus> rated = RatedFact.exact(Bus.PCI);

		assertEquals(Bus.PCI, rated.value().orElseThrow());
		assertEquals(Confidence.EXACT, rated.confidence());
	}

	@Test
	void representsNoMatchWithoutAValue() {
		final RatedFact<Bus> rated = RatedFact.none("Expected an expansion bus.");

		assertTrue(rated.value().isEmpty());
		assertEquals(Confidence.NONE, rated.confidence());
		assertEquals("Expected an expansion bus.", rated.explanation().orElseThrow());
	}

	@Test
	void rejectsSuccessfulResultsWithoutAValue() {
		assertThrows(NullPointerException.class, () -> RatedFact.<Bus> exact(null));
		assertThrows(NullPointerException.class, () -> RatedFact.<Bus> strong(null));
		assertThrows(NullPointerException.class, () -> RatedFact.<Bus> weak(null));
	}
}
