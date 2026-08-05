package com.retrocrawler.core.archive.clues;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Collects observed clues while enforcing the one-clue-per-key invariant, and
 * closes into immutable {@link Clues}.
 * <p>
 * This is the only place a clue is inspected. Every clue is checked once, as it
 * arrives, against everything accumulated so far. Nothing downstream re-inspects
 * what an accumulator has already closed.
 */
public final class ClueAccumulator {

	private final Map<String, Clue> cluesByKey = new LinkedHashMap<>();

	ClueAccumulator() {
	}

	ClueAccumulator(final Clues observed) {
		/*
		 * Clues hold one clue per key by construction, so seeding is a plain
		 * copy. Re-checking them here would be the redundant sanitizing this
		 * type exists to avoid.
		 */
		observed.forEach(clue -> cluesByKey.put(clue.key(), clue));
	}

	public ClueAccumulator addAll(final Iterable<Clue> clues) {
		Objects.requireNonNull(clues, "clues").forEach(this::add);
		return this;
	}

	public ClueAccumulator add(final Clue incoming) {
		Clue candidate = Objects.requireNonNull(incoming, "clue");
		Clue previous = cluesByKey.putIfAbsent(candidate.key(), candidate);

		/*
		 * Anonymous keys carry no semantics. In the very rare case of a random
		 * collision, preserve both observations by assigning the incoming clue
		 * a fresh key.
		 */
		while (previous != null && candidate.isAnonymous()) {
			candidate = Clue.of(candidate.value());
			previous = cluesByKey.putIfAbsent(candidate.key(), candidate);
		}

		if (previous != null) {
			throw new DuplicateClueException("Duplicate clue key '" + candidate.key()
					+ "'. One artifact may contain only one clue for a key. First values: " + previous.value()
					+ ", duplicate values: " + candidate.value() + ".");
		}
		return this;
	}

	/**
	 * Whether anything has been observed so far.
	 */
	public boolean isEmpty() {
		return cluesByKey.isEmpty();
	}

	/**
	 * Closes the accumulated observations into an immutable, ordered,
	 * key-unique value. The accumulator may keep collecting afterwards; the
	 * returned clues are unaffected.
	 */
	public Clues clues() {
		return new Clues(Collections.unmodifiableMap(new LinkedHashMap<>(cluesByKey)));
	}

}
