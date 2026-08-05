package com.retrocrawler.core.archive.clues;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Collects clues while enforcing the one-clue-per-key invariant. */
final class ClueAccumulator {

	private final Map<String, Clue> cluesByKey = new LinkedHashMap<>();

	ClueAccumulator() {
	}

	ClueAccumulator(final Collection<Clue> clues) {
		addAll(clues);
	}

	void addAll(final Collection<Clue> clues) {
		Objects.requireNonNull(clues, "clues").forEach(this::add);
	}

	void add(final Clue incoming) {
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
	}

	Set<Clue> clues() {
		return Set.copyOf(cluesByKey.values());
	}
}
