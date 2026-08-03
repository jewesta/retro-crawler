package com.retrocrawler.mycollection.clues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.mycollection.AttributeNames;

class BracketClueFinderTest {

	private final BracketClueFinder finder = new BracketClueFinder();

	@Test
	void ignoresFolderNamesWithoutBracketTags() {
		assertTrue(finder.find("Grouping folder").isEmpty());
	}

	@Test
	void keepsTitleTextRegardlessOfTagPosition() {
		final Set<Clue> clues = finder.find("[AGP] Example Graphics Board [200001]");

		assertEquals(Set.of("Example Graphics Board"), clue(clues, AttributeNames.TITLE).value());
		assertTrue(clues.stream().filter(Clue::isAnonymous).anyMatch(value -> value.value().contains("AGP")));
		assertTrue(clues.stream().filter(Clue::isAnonymous).anyMatch(value -> value.value().contains("200001")));
	}

	@Test
	void readsNamedGroups() {
		final Set<Clue> clues = finder.find("Board [SN 200003]");

		assertEquals(Set.of("200003"), clue(clues, AttributeNames.SERIAL_NUMBER).value());
	}

	@Test
	void normalizesNamedKeyCaseWithoutInterpretingItsVocabulary() {
		final Set<Clue> clues = finder.find("Board [trw 10510] [MAC 00-00-C0-0D-66-AB]");

		assertEquals(Set.of("10510"), clue(clues, AttributeNames.THE_RETRO_WEB_ID).value());
		assertEquals(Set.of("00-00-C0-0D-66-AB"), clue(clues, AttributeNames.MAC_ADDRESS).value());
	}

	@Test
	void distinguishesDecimalCommasFromListSeparators() {
		final Set<Clue> clues = finder.find("Memory [3,5] [ISA, PCI] [Alias one, two] [Set 2 x 1,125MB]");

		assertTrue(clues.stream().filter(Clue::isAnonymous)
				.anyMatch(value -> value.value().equals(Set.of("3,5"))));
		assertTrue(clues.stream().filter(Clue::isAnonymous)
				.anyMatch(value -> value.value().equals(Set.of("ISA", "PCI"))));
		assertEquals(Set.of("one", "two"), clue(clues, "alias").value());
		assertEquals(Set.of("2 x 1,125MB"), clue(clues, AttributeNames.CAPACITY_SET).value());
	}

	@Test
	void ignoresAnEmptyGroupAndDoesNotDeriveATitleFromIt() {
		assertTrue(finder.find("Board []").isEmpty());
	}

	@Test
	void retainsMalformedGroups() {
		final Set<Clue> clues = finder.find("Board [unfinished");

		assertTrue(clues.stream().filter(Clue::isAnonymous)
				.anyMatch(value -> value.value().contains("[unfinished")));
		assertEquals(Set.of("Board"), clue(clues, AttributeNames.TITLE).value());
	}

	private static Clue clue(final Set<Clue> clues, final String key) {
		return clues.stream().filter(clue -> key.equals(clue.key())).findFirst().orElseThrow();
	}
}
