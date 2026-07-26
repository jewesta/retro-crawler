package com.retrocrawler.mycollection.clues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.mycollection.AttributeNames;

class BracketPathClueFinderTest {

	private final BracketPathClueFinder finder = new BracketPathClueFinder();

	@Test
	void ignoresPathsWithoutBracketTags() {
		assertTrue(finder.find("Grouping folder").isEmpty());
	}

	@Test
	void keepsTitleTextRegardlessOfTagPosition() {
		final Set<Clue> clues = finder.find("[AGP] Example Graphics Board [200001]");

		assertEquals(Set.of("Example Graphics Board"), clue(clues, AttributeNames.TITLE).getValue());
		assertTrue(clues.stream().filter(Clue::isAnonymous).anyMatch(value -> value.getValue().contains("AGP")));
		assertTrue(clues.stream().filter(Clue::isAnonymous).anyMatch(value -> value.getValue().contains("200001")));
	}

	@Test
	void readsNamedGroups() {
		final Set<Clue> clues = finder.find("Board [SN 200003]");

		assertEquals(Set.of("200003"), clue(clues, "SN").getValue());
	}

	@Test
	void retainsEmptyAndMalformedGroups() {
		final Set<Clue> clues = finder.find("Board [] [unfinished");

		assertTrue(clues.stream().filter(Clue::isAnonymous).anyMatch(value -> value.getValue().contains("")));
		assertTrue(clues.stream().filter(Clue::isAnonymous)
				.anyMatch(value -> value.getValue().contains("[unfinished")));
	}

	private static Clue clue(final Set<Clue> clues, final String key) {
		return clues.stream().filter(clue -> key.equals(clue.getKey())).findFirst().orElseThrow();
	}
}
