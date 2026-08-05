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

	private static final Clues NONE = new Clues(Map.of(), Map.of());

	private final Map<String, Clue> cluesByKey;

	/**
	 * Where each clue was seen, for the finders that track offsets. Carried so
	 * that a duplicate rejected further downstream can still be drawn against
	 * the observation that claimed the key first — a finder accumulates
	 * privately, so this is the only way its positions outlive it.
	 * <p>
	 * Empty for clues built any other way, and dropped at the {@link Artifact}
	 * boundary: see {@link #withoutLocations()}.
	 */
	private final Map<String, ClueLocation> locationsByKey;

	Clues(final Map<String, Clue> cluesByKey, final Map<String, ClueLocation> locationsByKey) {
		this.cluesByKey = cluesByKey;
		this.locationsByKey = locationsByKey;
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
	 * Where the finder saw the clue claiming this key, when it tracked offsets.
	 */
	Optional<ClueLocation> locationOf(final String key) {
		return Optional.ofNullable(locationsByKey.get(key));
	}

	boolean hasLocations() {
		return !locationsByKey.isEmpty();
	}

	/**
	 * The same clues without their crawl-time positions.
	 * <p>
	 * An {@link Artifact} is the cache boundary, and diagnostics do not cross
	 * it: an artifact retrieved from the repository has no folder name or
	 * document left to point into, so a position that survived the cache would
	 * describe text nobody read this run. Dropping it here also keeps positions
	 * alive for exactly one folder's crawl rather than for the whole archive.
	 */
	Clues withoutLocations() {
		return hasLocations() ? new Clues(cluesByKey, Map.of()) : this;
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
