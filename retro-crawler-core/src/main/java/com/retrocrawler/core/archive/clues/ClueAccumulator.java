package com.retrocrawler.core.archive.clues;

import java.util.Collections;
import java.util.HashMap;
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
 * <p>
 * An accumulator also remembers where each accepted clue was spotted, so a
 * rejected duplicate can be reported against both observations instead of only
 * the second one. A finder that tracks offsets passes a {@link ClueLocation}
 * when it adds a clue; the framework names the {@link ClueSource} through
 * {@link #observing(ClueSource)} before it invokes a finder. Both are optional
 * and both die with the accumulator.
 */
public final class ClueAccumulator {

	private final Map<String, Clue> cluesByKey = new LinkedHashMap<>();

	private final Map<String, ClueSighting> sightingsByKey = new HashMap<>();

	private ClueSighting observing = ClueSighting.UNKNOWN;

	ClueAccumulator() {
	}

	ClueAccumulator(final Clues observed) {
		/*
		 * Clues hold one clue per key by construction, so seeding is a plain
		 * copy. Re-checking them here would be the redundant sanitizing this
		 * type exists to avoid.
		 */
		observed.forEach(clue -> {
			cluesByKey.put(clue.key(), clue);
			observed.locationOf(clue.key())
					.ifPresent(location -> sightingsByKey.put(clue.key(), new ClueSighting(null, location)));
		});
	}

	/**
	 * Names what is about to be read, so that clues accumulated from here on
	 * can be reported against their source. Applies until the next call.
	 */
	public ClueAccumulator observing(final ClueSource source) {
		this.observing = new ClueSighting(source, null);
		return this;
	}

	public ClueAccumulator addAll(final Iterable<Clue> clues) {
		Objects.requireNonNull(clues, "clues");
		/*
		 * A finder accumulates privately and hands back Clues, so the positions
		 * it tracked would otherwise die here. Take them over, or a conflict
		 * between two finders could only be reported by source.
		 */
		if (clues instanceof final Clues observed) {
			observed.forEach(clue -> add(clue, observed.locationOf(clue.key()).orElse(null)));
		} else {
			clues.forEach(this::add);
		}
		return this;
	}

	public ClueAccumulator add(final Clue incoming) {
		return add(incoming, null);
	}

	/**
	 * Adds a clue and records where the finder saw it, so that a later
	 * duplicate can point back at this observation.
	 */
	public ClueAccumulator add(final Clue incoming, final ClueLocation location) {
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
			throw new DuplicateClueException(previous, sighting(previous.key()), candidate,
					new ClueSighting(observing.source(), location));
		}

		final ClueSighting sighting = new ClueSighting(observing.source(), location);
		if (sighting.isKnown()) {
			sightingsByKey.put(candidate.key(), sighting);
		}
		return this;
	}

	private ClueSighting sighting(final String key) {
		return sightingsByKey.getOrDefault(key, ClueSighting.UNKNOWN);
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
		final Map<String, ClueLocation> locations = new HashMap<>();
		sightingsByKey.forEach((key, sighting) -> {
			if (sighting.location() != null) {
				locations.put(key, sighting.location());
			}
		});
		return new Clues(Collections.unmodifiableMap(new LinkedHashMap<>(cluesByKey)),
				Collections.unmodifiableMap(locations));
	}

}
