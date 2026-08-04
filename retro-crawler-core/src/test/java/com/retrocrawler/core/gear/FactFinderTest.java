package com.retrocrawler.core.gear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.gear.parser.EnumParser;
import com.retrocrawler.core.gear.parser.FactParseContext;

class FactFinderTest {

	private static final Path ARCHIVE_ROOT = Path.of("/archive");
	private static final FactParseContext CONTEXT = FactParseContext.located(ARCHIVE_ROOT,
			ARCHIVE_ROOT.resolve("gear"));

	private enum Bus {
		AGP,
		PCI
	}

	private final EnumParser<Bus> parser = new EnumParser<>(Bus.class);

	@Test
	void leavesConflictingValuesUnresolvedForScalarFacts() {
		final FactFinder finder = new FactFinder("bus", parser, Bus.class, false);

		assertTrue(finder.find(Clue.of("bus", Set.of("AGP", "PCI")), CONTEXT).isEmpty());
	}

	@Test
	void acceptsMultipleValuesForCollectionFacts() {
		final FactFinder finder = new FactFinder("bus", parser, Set.class, false);

		assertEquals(Set.of(Bus.AGP, Bus.PCI),
				finder.find(Clue.of("bus", Set.of("AGP", "PCI")), CONTEXT).orElseThrow().value());
	}

	@Test
	void collapsesDifferentSpellingsThatParseToTheSameScalarValue() {
		final FactFinder finder = new FactFinder("bus", parser, Bus.class, false);

		assertEquals(Set.of(Bus.AGP), finder.find(Clue.of("bus", Set.of("AGP", "agp")), CONTEXT).orElseThrow().value());
	}

	@Test
	void doesNotCreateAFactWithoutAValue() {
		final FactFinder finder = new FactFinder("bus", parser, Bus.class, false);

		assertTrue(finder.find(Clue.missingValue("bus"), CONTEXT).isEmpty());
	}

}
