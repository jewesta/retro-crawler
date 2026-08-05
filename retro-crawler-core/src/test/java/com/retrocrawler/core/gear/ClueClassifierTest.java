package com.retrocrawler.core.gear;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.DuplicateClueException;

class ClueClassifierTest {

	@Test
	void classifiesAnAnonymousKnownKeyAsAMissingValueClue() {
		final ClueClassifier classifier = new ClueClassifier(Set.of("sn"));

		final Set<Clue> clues = classifier.classify(Set.of(Clue.of("SN"), Clue.of("AGP"), Clue.of("")));

		assertTrue(clue(clues, "sn").isMissingValue());
		assertTrue(clues.stream().filter(Clue::isAnonymous).anyMatch(value -> value.value().equals(Set.of("AGP"))));
		assertFalse(clues.stream().anyMatch(value -> value.value().contains("")));
	}

	@Test
	void rejectsARealValueAndMissingValueFromSeparateClues() {
		final ClueClassifier classifier = new ClueClassifier(Set.of("sn"));

		assertThrows(DuplicateClueException.class,
				() -> classifier.classify(Set.of(Clue.of("SN"), Clue.of("sn", "12345"))));
	}

	private static Clue clue(final Set<Clue> clues, final String key) {
		return clues.stream().filter(candidate -> key.equals(candidate.key())).findFirst().orElseThrow();
	}
}
