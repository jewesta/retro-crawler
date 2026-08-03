package com.retrocrawler.demo.clues;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Clue;

class SquareBracketsClueFinderTest {

	@Test
	void normalizesEveryNamedKeyWithoutKnowingItsVocabulary() {
		final Set<Clue> clues = new SquareBracketsClueFinder()
				.find("Manual [ISBN 978-0-306-40615-7] [Alias one, two]");

		assertEquals(Set.of("978-0-306-40615-7"), clue(clues, "isbn").value());
		assertEquals(Set.of("one", "two"), clue(clues, "alias").value());
	}

	private static Clue clue(final Set<Clue> clues, final String key) {
		return clues.stream()
				.filter(value -> value.key().equals(key))
				.findFirst()
				.orElseThrow();
	}
}
