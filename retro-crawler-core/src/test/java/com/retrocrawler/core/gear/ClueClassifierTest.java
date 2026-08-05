package com.retrocrawler.core.gear;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.Clues;
import com.retrocrawler.core.archive.clues.DuplicateClueException;

class ClueClassifierTest {

	@Test
	void classifiesAnAnonymousKnownKeyAsAMissingValueClue() {
		final ClueClassifier classifier = new ClueClassifier(Set.of("sn"));

		final Clues clues = classifier.classify(Clues.of(Clue.of("SN"), Clue.of("AGP"), Clue.of("")));

		assertTrue(clues.get("sn").orElseThrow().isMissingValue());
		assertTrue(clues.stream().filter(Clue::isAnonymous).anyMatch(value -> value.value().equals(Set.of("AGP"))));
		assertFalse(clues.stream().anyMatch(value -> value.value().contains("")));
	}

	@Test
	void rejectsARealValueAndMissingValueFromSeparateClues() {
		final ClueClassifier classifier = new ClueClassifier(Set.of("sn"));
		final Clues observed = Clues.of(Clue.of("SN"), Clue.of("sn", "12345"));

		final DuplicateClueException failure = assertThrows(DuplicateClueException.class,
				() -> classifier.classify(observed));

		/*
		 * The rejected observation is named in its raw form. Classifying strips
		 * the anonymous clue down to a missing-value clue, so the message would
		 * otherwise not show what the archive actually said.
		 */
		assertTrue(failure.getMessage().contains("sn"));
		assertTrue(failure.getMessage().contains("SN"));
	}
}
