package com.retrocrawler.core.archive.clues;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The clues observed at one archive location, in observation order, holding
 * exactly one clue per key.
 * <p>
 * Every clue key has exactly one authority within an artifact. {@code Clues}
 * carries that invariant in the type rather than in whichever collaborator
 * happens to assemble the clues. An instance can only come into existence
 * through a {@link ClueAccumulator}, so each clue is inspected once, where it is
 * observed, and never re-inspected while travelling from a
 * {@link ClueFinder} to an {@link Artifact}.
 * <p>
 * A {@code Set<Clue>} cannot express this. {@link Clue} deliberately keeps
 * identity equality so that conflicting observations survive long enough to be
 * rejected, which means a set would promise a uniqueness it never enforces and
 * would say nothing at all about the key-level rule that actually applies.
 */
public final class Clues implements Iterable<Clue> {

	private static final Clues NONE = new Clues(Map.of());

	private final Map<String, Clue> cluesByKey;

	Clues(final Map<String, Clue> cluesByKey) {
		this.cluesByKey = cluesByKey;
	}

	/**
	 * No clue observed. A folder with no clues establishes no artifact.
	 */
	public static Clues none() {
		return NONE;
	}

	public static Clues of(final Clue... clues) {
		return of(List.of(Objects.requireNonNull(clues, "clues")));
	}

	public static Clues of(final Iterable<Clue> clues) {
		return accumulator().addAll(clues).clues();
	}

	/**
	 * Opens a fresh accumulator for a finder that observes its clues one at a
	 * time.
	 */
	public static ClueAccumulator accumulator() {
		return new ClueAccumulator();
	}

	/**
	 * Continues accumulating from clues already observed. Seeding is a copy, not
	 * a second inspection: {@code observed} is key-unique by construction.
	 */
	public static ClueAccumulator accumulator(final Clues observed) {
		return new ClueAccumulator(Objects.requireNonNull(observed, "observed"));
	}

	/**
	 * Returns these clues together with the incoming ones. Only the incoming
	 * clues are inspected, and they are inspected against the receiver, so a key
	 * claimed on both sides is still rejected.
	 */
	public Clues and(final Clues incoming) {
		Objects.requireNonNull(incoming, "incoming");
		if (incoming.isEmpty()) {
			return this;
		}
		if (isEmpty()) {
			return incoming;
		}
		return accumulator(this).addAll(incoming).clues();
	}

	public Clues and(final Clue incoming) {
		return accumulator(this).add(incoming).clues();
	}

	/**
	 * Returns the single clue claiming the given key, if any.
	 */
	public Optional<Clue> get(final String key) {
		return Optional.ofNullable(cluesByKey.get(Objects.requireNonNull(key, "key")));
	}

	public boolean contains(final String key) {
		return cluesByKey.containsKey(Objects.requireNonNull(key, "key"));
	}

	public int size() {
		return cluesByKey.size();
	}

	public boolean isEmpty() {
		return cluesByKey.isEmpty();
	}

	public Stream<Clue> stream() {
		return cluesByKey.values().stream();
	}

	@Override
	public Iterator<Clue> iterator() {
		return cluesByKey.values().iterator();
	}

	@Override
	public String toString() {
		return cluesByKey.values().toString();
	}

}
