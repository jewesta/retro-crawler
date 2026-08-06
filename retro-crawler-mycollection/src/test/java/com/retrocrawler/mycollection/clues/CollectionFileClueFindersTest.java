package com.retrocrawler.mycollection.clues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.ClueFindingException;
import com.retrocrawler.core.archive.clues.Clues;
import com.retrocrawler.mycollection.AttributeNames;

class CollectionFileClueFindersTest {

	@Test
	void importsTheCompleteMarkdownDocumentAsDesc() {
		final RetroMarkdownClueFinder finder = new RetroMarkdownClueFinder();
		final String markdown = "# Notes\n\nA human-maintained description.\n";

		final Clues clues = finder.find(input(markdown));

		assertTrue(finder.matches("retro.md"));
		assertEquals(Set.of(markdown), clues.get(AttributeNames.DESC).orElseThrow().value());
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

		final Clues clues = finder.find(input(markdown));

		assertEquals(Set.of("120 EUR"), clues.get(AttributeNames.PRICE).orElseThrow().value());
		assertEquals(Set.of("200001", "200002,200003"), clues.get(AttributeNames.LOT).orElseThrow().value());
		assertEquals(Set.of("123"), clues.get(AttributeNames.FCC_ID).orElseThrow().value());
		assertEquals(Set.of("defekt"), clues.get(AttributeNames.HEALTH).orElseThrow().value());
		assertEquals(Set.of("post"), clues.get(AttributeNames.TESTED).orElseThrow().value());
		assertEquals(Set.of("Gerät läuft wieder."), clues.get(AttributeNames.DESC).orElseThrow().value());
	}

	@Test
	void rejectsMalformedOrMisplacedFrontMatterPointingAtTheOffendingLine() {
		final RetroMarkdownClueFinder finder = new RetroMarkdownClueFinder();

		final ClueFindingException missingColon = assertThrows(ClueFindingException.class,
				() -> finder.find(input("---\nprice 120 EUR\n---\n")));
		assertEquals(2, missingColon.location().orElseThrow().line());
		assertEquals("price 120 EUR", missingColon.location().orElseThrow().excerpt());

		final ClueFindingException misplacedDesc = assertThrows(ClueFindingException.class,
				() -> finder.find(input("---\nfcc: 123\ndesc: Wrong level\n---\n")));
		assertEquals(3, misplacedDesc.location().orElseThrow().line());
		assertEquals("desc: Wrong level", misplacedDesc.location().orElseThrow().excerpt());

		final ClueFindingException unclosed = assertThrows(ClueFindingException.class,
				() -> finder.find(input("---\nprice: 120 EUR\n")));
		assertEquals(1, unclosed.location().orElseThrow().line());
	}

	/**
	 * Two front matter lines for one key are one authority supplying several
	 * values, not a conflict, so the file merges them itself.
	 */
	@Test
	void mergesRepeatedFrontMatterLinesForOneKey() {
		final Clues clues = new RetroMarkdownClueFinder().find(input("---\nlot: 200001\nlot: 200002\n---\n"));

		assertEquals(Set.of("200001", "200002"), clues.get(AttributeNames.LOT).orElseThrow().value());
	}

	@Test
	void recognizesOnlyTheThreeExactStandardImageNames() {
		final StandardImageClueFinder finder = new StandardImageClueFinder();

		final Clues clues = finder.find(List.of(Path.of("ANGLED.JPEG"), Path.of("front.jpeg"), Path.of("back.jpeg"),
				Path.of("front.jpg"), Path.of("overview.jpeg")));

		assertEquals(Set.of("ANGLED.JPEG"), clues.get(AttributeNames.IMAGE_ANGLED).orElseThrow().value());
		assertEquals(Set.of("front.jpeg"), clues.get(AttributeNames.IMAGE_FRONT).orElseThrow().value());
		assertEquals(Set.of("back.jpeg"), clues.get(AttributeNames.IMAGE_BACK).orElseThrow().value());
		assertEquals(3, clues.size());
	}

	@Test
	void recognizesAndAggregatesFloppyImageIdsAtTheStartOfFileNames() {
		final FloppyImageClueFinder finder = new FloppyImageClueFinder();

		final Clues clues = finder.find(
				List.of(Path.of("FD-0007.img"), Path.of("fd-0008 Boot disk.ima"), Path.of("Copy of FD-0009.img")));

		assertEquals(Set.of("FD-0007", "FD-0008"), clues.get(AttributeNames.FLOPPY_IMAGE_ID).orElseThrow().value());
		assertEquals(Set.of("FD-0007.img", "fd-0008 Boot disk.ima"), clues.get(AttributeNames.FLOPPY_IMAGES).orElseThrow().value());
	}

	private static ByteArrayInputStream input(final String value) {
		return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
	}
}
