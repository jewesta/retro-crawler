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

		assertEquals(Set.of("200003"), clue(clues, AttributeNames.SERIAL_NUMBER).getValue());
	}

	@Test
	void normalizesNamedKeyCaseWithoutInterpretingItsVocabulary() {
		final Set<Clue> clues = finder.find("Board [trw 10510] [MAC 00-00-C0-0D-66-AB]");

		assertEquals(Set.of("10510"), clue(clues, AttributeNames.THE_RETRO_WEB_ID).getValue());
		assertEquals(Set.of("00-00-C0-0D-66-AB"), clue(clues, AttributeNames.MAC_ADDRESS).getValue());
	}

	@Test
	void distinguishesDecimalCommasFromListSeparators() {
		final Set<Clue> clues = finder.find("Memory [3,5] [ISA, PCI] [Alias one, two] [Set 2 x 1,125MB]");

		assertTrue(clues.stream().filter(Clue::isAnonymous)
				.anyMatch(value -> value.getValue().equals(Set.of("3,5"))));
		assertTrue(clues.stream().filter(Clue::isAnonymous)
				.anyMatch(value -> value.getValue().equals(Set.of("ISA", "PCI"))));
		assertEquals(Set.of("one", "two"), clue(clues, "alias").getValue());
		assertEquals(Set.of("2 x 1,125MB"), clue(clues, AttributeNames.RAM_SET).getValue());
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
