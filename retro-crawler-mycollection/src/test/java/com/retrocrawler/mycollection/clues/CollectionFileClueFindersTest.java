package com.retrocrawler.mycollection.clues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.ClueFileIOException;
import com.retrocrawler.mycollection.AttributeNames;

class CollectionFileClueFindersTest {

	@Test
	void importsTheCompleteMarkdownDocumentAsDesc() {
		final RetroMarkdownClueFinder finder = new RetroMarkdownClueFinder();
		final String markdown = "# Notes\n\nA human-maintained description.\n";

		final Set<Clue> clues = finder.find(input(markdown));

		assertTrue(finder.matches("retro.md"));
		assertEquals(Set.of(markdown), clue(clues, AttributeNames.DESC).value());
	}

	@Test
	void importsFlatFrontMatterAndTheMarkdownBody() {
		final RetroMarkdownClueFinder finder = new RetroMarkdownClueFinder();
		final String markdown = """
			---
			price: 120 EUR
			lot: 200001, 200002,200003
			fcc: 123
			health: defekt
			tested: post
			---
			Gerät läuft wieder.
			""";

		final Set<Clue> clues = finder.find(input(markdown));

		assertEquals(Set.of("120 EUR"), clue(clues, AttributeNames.PRICE).value());
		assertEquals(Set.of("200001", "200002,200003"), clue(clues, AttributeNames.LOT).value());
		assertEquals(Set.of("123"), clue(clues, AttributeNames.FCC_ID).value());
		assertEquals(Set.of("defekt"), clue(clues, AttributeNames.HEALTH).value());
		assertEquals(Set.of("post"), clue(clues, AttributeNames.TESTED).value());
		assertEquals(Set.of("Gerät läuft wieder."), clue(clues, AttributeNames.DESC).value());
	}

	@Test
	void rejectsMalformedOrMisplacedFrontMatter() {
		final RetroMarkdownClueFinder finder = new RetroMarkdownClueFinder();

		assertThrows(ClueFileIOException.class, () -> finder.find(input("---\nprice 120 EUR\n---\n")));
		assertThrows(ClueFileIOException.class, () -> finder.find(input("---\ndesc: Wrong level\n---\n")));
		assertThrows(ClueFileIOException.class, () -> finder.find(input("---\nprice: 120 EUR\n")));
	}

	@Test
	void recognizesOnlyTheThreeExactStandardImageNames() {
		final StandardImageClueFinder finder = new StandardImageClueFinder();

		final Set<Clue> clues = finder.find(List.of(Path.of("ANGLED.JPEG"), Path.of("front.jpeg"), Path.of("back.jpeg"),
				Path.of("front.jpg"), Path.of("overview.jpeg")));

		assertEquals(Set.of("ANGLED.JPEG"), clue(clues, AttributeNames.IMAGE_ANGLED).value());
		assertEquals(Set.of("front.jpeg"), clue(clues, AttributeNames.IMAGE_FRONT).value());
		assertEquals(Set.of("back.jpeg"), clue(clues, AttributeNames.IMAGE_BACK).value());
		assertEquals(3, clues.size());
	}

	@Test
	void recognizesAndAggregatesFloppyImageIdsAtTheStartOfFileNames() {
		final FloppyImageClueFinder finder = new FloppyImageClueFinder();

		final Set<Clue> clues = finder.find(
				List.of(Path.of("FD-0007.img"), Path.of("fd-0008 Boot disk.ima"), Path.of("Copy of FD-0009.img")));

		assertEquals(Set.of("FD-0007", "FD-0008"), clue(clues, AttributeNames.FLOPPY_IMAGE_ID).value());
		assertEquals(Set.of("FD-0007.img", "fd-0008 Boot disk.ima"), clue(clues, AttributeNames.FLOPPY_IMAGES).value());
	}

	private static ByteArrayInputStream input(final String value) {
		return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
	}

	private static Clue clue(final Set<Clue> clues, final String key) {
		final java.util.Map<String, Clue> byKey = clues.stream()
				.collect(Collectors.toMap(Clue::key, java.util.function.Function.identity()));
		return byKey.get(key);
	}
}
