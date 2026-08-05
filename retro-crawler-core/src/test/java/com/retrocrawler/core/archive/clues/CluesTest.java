package com.retrocrawler.core.archive.clues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class CluesTest {

	@Test
	void rejectsMoreThanOneClueForTheSameKey() {
		final Clue folderClue = Clue.of("bus", "ISA");
		final Clue fileClue = Clue.of("bus", "PCI");

		final DuplicateClueException failure = assertThrows(DuplicateClueException.class,
				() -> Clues.of(folderClue, fileClue));

		assertTrue(failure.getMessage().contains("bus"));
		assertTrue(failure.getMessage().contains("ISA"));
		assertTrue(failure.getMessage().contains("PCI"));
	}

	@Test
	void rejectsADuplicateKeyArrivingLater() {
		final Clues observed = Clues.of(Clue.of("bus", "ISA"));

		assertThrows(DuplicateClueException.class, () -> observed.and(Clue.of("bus", "PCI")));
	}

	@Test
	void rekeysCollidingAnonymousCluesInsteadOfMergingTheirValues() {
		final Clue first = new Clue("_deadbeef", Set.of("first"));
		final Clue second = new Clue("_deadbeef", Set.of("second"));

		final Clues clues = Clues.of(first, second);

		assertEquals(2, clues.size());
		assertTrue(clues.stream().allMatch(clue -> clue.value().size() == 1));
		assertEquals(Set.of("first", "second"),
				clues.stream().flatMap(clue -> clue.value().stream()).collect(Collectors.toSet()));
		final Set<String> keys = clues.stream().map(Clue::key).collect(Collectors.toSet());
		assertEquals(2, keys.size());
		assertTrue(keys.contains("_deadbeef"));
		assertTrue(keys.stream().allMatch(key -> key.matches("_[a-z0-9]{8}")));
	}

	@Test
	void keepsObservationOrder() {
		final Clues clues = Clues.of(Clue.of("third", "3"), Clue.of("first", "1"), Clue.of("second", "2"));

		assertEquals(List.of("third", "first", "second"), clues.stream().map(Clue::key).toList());
	}

	@Test
	void findsTheSingleClueClaimingAKey() {
		final Clues clues = Clues.of(Clue.of("bus", "ISA"), Clue.of("title", "Sound card"));

		assertEquals(Set.of("ISA"), clues.get("bus").orElseThrow().value());
		assertTrue(clues.contains("title"));
		assertTrue(clues.get("absent").isEmpty());
	}

	@Test
	void cannotBeMutatedThroughItsIterator() {
		final Clues clues = Clues.of(Clue.of("bus", "ISA"));
		final var iterator = clues.iterator();
		iterator.next();

		assertThrows(UnsupportedOperationException.class, iterator::remove);
	}

	@Test
	void isUnaffectedByLaterAccumulation() {
		final ClueAccumulator accumulator = Clues.accumulator();
		accumulator.add(Clue.of("bus", "ISA"));
		final Clues closed = accumulator.clues();

		accumulator.add(Clue.of("title", "Sound card"));

		assertEquals(1, closed.size());
		assertEquals(2, accumulator.clues().size());
	}

	@Test
	void returnsTheReceiverWhenNothingIsAdded() {
		final Clues clues = Clues.of(Clue.of("bus", "ISA"));

		assertSame(clues, clues.and(Clues.none()));
		assertSame(clues, Clues.none().and(clues));
	}
}
