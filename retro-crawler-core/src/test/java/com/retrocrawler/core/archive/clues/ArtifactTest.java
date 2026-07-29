package com.retrocrawler.core.archive.clues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ArtifactTest {

	@Test
	void protectsCachedCluesFromExternalMutation() {
		final Set<Clue> clues = new HashSet<>(Set.of(Clue.of("bus", "AGP")));
		final Artifact artifact = new Artifact(clues);

		clues.clear();

		assertEquals(1, artifact.getClues().size());
		assertThrows(UnsupportedOperationException.class, () -> artifact.getClues().clear());
	}

	@Test
	void protectsRawClueValuesFromExternalMutation() {
		final Set<String> values = new HashSet<>(Set.of("AGP"));
		final Clue clue = Clue.of("bus", values);

		values.add("PCI");

		assertEquals(Set.of("AGP"), clue.getValue());
		assertThrows(UnsupportedOperationException.class, () -> clue.getValue().add("PCI"));
	}
}
