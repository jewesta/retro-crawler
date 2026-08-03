package com.retrocrawler.core.gear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.Confidence;

class FactTest {

	@Test
	void rejectsFactsWithoutAnInterpretedValue() {
		assertThrows(IllegalArgumentException.class,
				() -> new Fact("bus", Set.of(), Confidence.EXACT, Clue.missingValue("bus")));
	}

	@Test
	void protectsInterpretedValuesFromExternalMutation() {
		final Set<Object> values = new HashSet<>(Set.of("AGP"));
		final Fact fact = new Fact("bus", values, Confidence.EXACT, Clue.of("bus", "AGP"));

		values.add("PCI");

		assertEquals(Set.of("AGP"), fact.value());
		assertThrows(UnsupportedOperationException.class, () -> fact.value().add("PCI"));
	}
}
