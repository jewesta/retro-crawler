package com.retrocrawler.mycollection.clues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.Clues;
import com.retrocrawler.core.archive.clues.DuplicateClueException;
import com.retrocrawler.mycollection.AttributeNames;

class BracketClueFinderTest {

	private final BracketClueFinder finder = new BracketClueFinder();

	@Test
	void ignoresFolderNamesWithoutBracketTags() {
		assertTrue(finder.find("Grouping folder").isEmpty());
	}

	@Test
	void keepsTitleTextRegardlessOfTagPosition() {
		final Clues clues = finder.find("[AGP] Example Graphics Board [200001]");

		assertEquals(Set.of("Example Graphics Board"), clues.get(AttributeNames.TITLE).orElseThrow().value());
		assertTrue(clues.stream().filter(Clue::isAnonymous).anyMatch(value -> value.value().contains("AGP")));
		assertTrue(clues.stream().filter(Clue::isAnonymous).anyMatch(value -> value.value().contains("200001")));
	}

	@Test
	void readsNamedGroups() {
		final Clues clues = finder.find("Board [SN 200003]");

		assertEquals(Set.of("200003"), clues.get(AttributeNames.SERIAL_NUMBER).orElseThrow().value());
	}

	@Test
	void normalizesNamedKeyCaseWithoutInterpretingItsVocabulary() {
		final Clues clues = finder.find("Board [trw 10510] [MAC 00-00-C0-0D-66-AB]");

		assertEquals(Set.of("10510"), clues.get(AttributeNames.THE_RETRO_WEB_ID).orElseThrow().value());
		assertEquals(Set.of("00-00-C0-0D-66-AB"), clues.get(AttributeNames.MAC_ADDRESS).orElseThrow().value());
	}

	@Test
	void distinguishesDecimalCommasFromListSeparators() {
		final Clues clues = finder.find("Memory [3,5] [ISA, PCI] [Alias one, two] [Set 2 x 1,125MB]");

		assertTrue(clues.stream().filter(Clue::isAnonymous).anyMatch(value -> value.value().equals(Set.of("3,5"))));
		assertTrue(
				clues.stream().filter(Clue::isAnonymous).anyMatch(value -> value.value().equals(Set.of("ISA", "PCI"))));
		assertEquals(Set.of("one", "two"), clues.get("alias").orElseThrow().value());
		assertEquals(Set.of("2 x 1,125MB"), clues.get(AttributeNames.CAPACITY_SET).orElseThrow().value());
	}

	@Test
	void ignoresAnEmptyGroupAndDoesNotDeriveATitleFromIt() {
		assertTrue(finder.find("Board []").isEmpty());
	}

	@Test
	void pointsAtBothBracketGroupsWhenTheyClaimOneKey() {
		final DuplicateClueException failure = assertThrows(DuplicateClueException.class,
				() -> finder.find("Example Board [bus ISA] [200001] [bus PCI]"));

		assertEquals("""
				Duplicate clue key 'bus'. One artifact may contain only one clue for a key. \
				First values: [ISA], duplicate values: [PCI].
				  Example Board [bus ISA] [200001] [bus PCI]
				                ^^^^^^^^^ first
				                                   ^^^^^^^^^ duplicate""", failure.getMessage());
	}

	@Test
	void retainsMalformedGroups() {
		final Clues clues = finder.find("Board [unfinished");

		assertTrue(clues.stream().filter(Clue::isAnonymous).anyMatch(value -> value.value().contains("[unfinished")));
		assertEquals(Set.of("Board"), clues.get(AttributeNames.TITLE).orElseThrow().value());
	}

}
