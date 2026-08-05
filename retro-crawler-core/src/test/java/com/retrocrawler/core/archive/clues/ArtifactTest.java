package com.retrocrawler.core.archive.clues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ArtifactTest {

	@Test
	void protectsCachedCluesFromExternalMutation() {
		final Set<Clue> clues = new HashSet<>(Set.of(Clue.of("bus", "AGP")));
		final Artifact artifact = new Artifact(clues);

		clues.clear();

		assertEquals(1, artifact.clues().size());
		assertThrows(UnsupportedOperationException.class, () -> artifact.clues().clear());
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
	void rejectsMoreThanOneClueForTheSameKey() {
		final Clue folderClue = Clue.of("bus", "ISA");
		final Clue fileClue = Clue.of("bus", "PCI");

		final DuplicateClueException failure = assertThrows(DuplicateClueException.class,
				() -> new Artifact(Set.of(folderClue, fileClue)));

		assertTrue(failure.getMessage().contains("bus"));
		assertTrue(failure.getMessage().contains("ISA"));
		assertTrue(failure.getMessage().contains("PCI"));
	}
}
