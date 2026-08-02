package com.retrocrawler.core.gear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.core.gear.parser.EnumParser;

class FactFinderTest {

	private enum Bus {
		AGP,
		PCI
	}

	private final EnumParser<Bus> parser = new EnumParser<>(Bus.class);

	@Test
	void leavesConflictingValuesUnresolvedForScalarFacts() {
		final FactFinder finder = new FactFinder("bus", parser, Bus.class, false);

		assertTrue(finder.find(Clue.of("bus", Set.of("AGP", "PCI"))).isEmpty());
	}

	@Test
	void acceptsMultipleValuesForCollectionFacts() {
		final FactFinder finder = new FactFinder("bus", parser, Set.class, false);

		assertEquals(Set.of(Bus.AGP, Bus.PCI),
				finder.find(Clue.of("bus", Set.of("AGP", "PCI"))).orElseThrow().getValue());
	}

	@Test
	void collapsesDifferentSpellingsThatParseToTheSameScalarValue() {
		final FactFinder finder = new FactFinder("bus", parser, Bus.class, false);

		assertEquals(Set.of(Bus.AGP),
				finder.find(Clue.of("bus", Set.of("AGP", "agp"))).orElseThrow().getValue());
	}

	@Test
	void doesNotCreateAFactWithoutAValue() {
		final FactFinder finder = new FactFinder("bus", parser, Bus.class, false);

		assertTrue(finder.find(Clue.missingValue("bus")).isEmpty());
	}

	@Test
	void flattensMultipleValuesParsedFromOneObservationForCollectionFacts() {
		final FactParser compoundParser = raw -> RatedFact.exact(Set.of(Bus.AGP, Bus.PCI));
		final FactFinder finder = new FactFinder("bus", compoundParser, Set.class, false);

		assertEquals(Set.of(Bus.AGP, Bus.PCI),
				finder.find(Clue.of("bus", "AGP/PCI")).orElseThrow().getValue());
	}

	@Test
	void rejectsMultipleValuesParsedFromOneObservationForScalarFacts() {
		final FactParser compoundParser = raw -> RatedFact.exact(Set.of(Bus.AGP, Bus.PCI));
		final FactFinder finder = new FactFinder("bus", compoundParser, Bus.class, false);

		assertTrue(finder.find(Clue.of("bus", "AGP/PCI")).isEmpty());
	}
}
