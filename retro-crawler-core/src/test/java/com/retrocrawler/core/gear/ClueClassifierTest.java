package com.retrocrawler.core.gear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.Clues;

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
	void preservesAnAnonymousMarkerWithoutCompetingWithAnExplicitValue() {
		final ClueClassifier classifier = new ClueClassifier(Set.of("sn"));

		for (final Clues observed : List.of(Clues.of(Clue.of("SN"), Clue.of("sn", "12345")),
				Clues.of(Clue.of("sn", "12345"), Clue.of("SN")))) {
			final Clues classified = classifier.classify(observed);

			assertEquals(Set.of("12345"), classified.get("sn").orElseThrow().value());
			assertTrue(
					classified.stream().anyMatch(value -> value.isAnonymous() && value.value().equals(Set.of("SN"))));
		}
	}
}
