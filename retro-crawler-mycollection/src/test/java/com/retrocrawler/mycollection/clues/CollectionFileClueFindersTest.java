package com.retrocrawler.mycollection.clues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.mycollection.AttributeNames;

class CollectionFileClueFindersTest {

	@Test
	void importsLegacyPropertiesAsKeyedClues() {
		final RetroPropertiesClueFinder finder = new RetroPropertiesClueFinder();

		final Set<Clue> clues = finder.find(input("bus=AGP\ntitle=Example card\n"));

		assertTrue(finder.matches("RETRO.PROPERTIES"));
		assertEquals(Set.of("AGP"), clue(clues, AttributeNames.BUS).getValue());
		assertEquals(Set.of("Example card"), clue(clues, AttributeNames.TITLE).getValue());
	}

	@Test
	void importsTheCompleteMarkdownDocumentAsDescription() {
		final RetroMarkdownClueFinder finder = new RetroMarkdownClueFinder();
		final String markdown = "# Notes\n\nA human-maintained description.\n";

		final Set<Clue> clues = finder.find(input(markdown));

		assertTrue(finder.matches("retro.md"));
		assertEquals(Set.of(markdown), clue(clues, AttributeNames.DESCRIPTION).getValue());
	}

	@Test
	void recognizesOnlyTheThreeExactStandardImageNames() {
		final StandardImageClueFinder finder = new StandardImageClueFinder();
		final Path folder = Path.of("gear");

		final Set<Clue> clues = finder.find(List.of(
				folder.resolve("ANGLED.JPEG"),
				folder.resolve("front.jpeg"),
				folder.resolve("back.jpeg"),
				folder.resolve("front.jpg"),
				folder.resolve("overview.jpeg")));

		assertEquals(Set.of(folder.resolve("ANGLED.JPEG").toString()),
				clue(clues, AttributeNames.IMAGE_ANGLED).getValue());
		assertEquals(Set.of(folder.resolve("front.jpeg").toString()),
				clue(clues, AttributeNames.IMAGE_FRONT).getValue());
		assertEquals(Set.of(folder.resolve("back.jpeg").toString()),
				clue(clues, AttributeNames.IMAGE_BACK).getValue());
		assertEquals(3, clues.size());
	}

	@Test
	void recognizesAndAggregatesFloppyImageIdsAtTheStartOfFileNames() {
		final FloppyImageClueFinder finder = new FloppyImageClueFinder();
		final Path folder = Path.of("gear");

		final Set<Clue> clues = finder.find(List.of(
				folder.resolve("FD-0007.img"),
				folder.resolve("fd-0008 Boot disk.ima"),
				folder.resolve("Copy of FD-0009.img")));

		assertEquals(Set.of("FD-0007", "FD-0008"), clue(clues, AttributeNames.FLOPPY_IMAGE_ID).getValue());
		assertEquals(Set.of(folder.resolve("FD-0007.img").toString(),
				folder.resolve("fd-0008 Boot disk.ima").toString()),
				clue(clues, AttributeNames.FLOPPY_IMAGES).getValue());
	}

	private static ByteArrayInputStream input(final String value) {
		return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
	}

	private static Clue clue(final Set<Clue> clues, final String key) {
		final java.util.Map<String, Clue> byKey = clues.stream()
				.collect(Collectors.toMap(Clue::getKey, java.util.function.Function.identity()));
		return byKey.get(key);
	}
}
