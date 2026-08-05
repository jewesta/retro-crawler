package com.retrocrawler.core.archive.clues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ArtifactTest {

	@Test
	void protectsCachedCluesFromExternalMutation() {
		final List<Clue> observed = new java.util.ArrayList<>(List.of(Clue.of("bus", "AGP")));
		final Artifact artifact = new Artifact(Clues.of(observed));

		observed.clear();

		assertEquals(1, artifact.clues().size());
	}

	@Test
	void protectsRawClueValuesFromExternalMutation() {
		final Set<String> values = new HashSet<>(Set.of("AGP"));
		final Clue clue = Clue.of("bus", values);

		values.add("PCI");

		assertEquals(Set.of("AGP"), clue.value());
		assertThrows(UnsupportedOperationException.class, () -> clue.value().add("PCI"));
	}

	@Test
	void holdsTheCluesItWasGivenWithoutRebuildingThem() {
		final Clues clues = Clues.of(Clue.of("bus", "AGP"));

		assertSame(clues, new Artifact(clues).clues());
	}

	@Test
	void rejectsAnArtifactWithoutAClue() {
		assertThrows(IllegalArgumentException.class, () -> new Artifact(Clues.none()));
		assertThrows(NullPointerException.class, () -> new Artifact(null));
	}
}
