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
 * arrives, against everything accumulated so far. Nothing downstream
 * re-inspects what an accumulator has already closed.
 * <p>
 * An accumulator also remembers where each accepted clue was spotted, so a
 * rejected duplicate can be reported against both observations instead of only
 * the second one. A finder that tracks offsets passes a {@link ClueLocation}
 * when it adds a clue. Positions die with the accumulator; finder and resource
 * provenance live on the clue itself.
 */
public final class ClueAccumulator {

	private final Map<String, Clue> cluesByKey = new LinkedHashMap<>();

	private final Map<String, ClueLocation> locationsByKey = new HashMap<>();

	private String finder;

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
			observed.locationOf(clue.key()).ifPresent(location -> locationsByKey.put(clue.key(), location));
		});
	}

	/**
	 * Names the finder whose returned clues are about to be accumulated. The
	 * name is attached to every incoming clue and applies until the next call.
	 */
	public ClueAccumulator foundBy(final String finderName) {
		this.finder = Objects.requireNonNull(finderName, "finderName");
		return this;
	}

	public ClueAccumulator addAll(final Iterable<Clue> clues) {
		Objects.requireNonNull(clues, "clues");
		final Map<String, Clue> cluesBefore = new LinkedHashMap<>(cluesByKey);
		final Map<String, ClueLocation> locationsBefore = new HashMap<>(locationsByKey);
		/*
		 * A finder accumulates privately and hands back Clues, so the positions
		 * it tracked would otherwise die here. Take them over, or a conflict
		 * between two finders could only be reported by source.
		 *
		 * The hand-off is atomic. Fail-late crawling continues with the next
		 * independent finder, which must not see clues partially accepted from
		 * a finder whose complete result was rejected.
		 */
		try {
			if (clues instanceof final Clues observed) {
				observed.forEach(clue -> add(clue, observed.locationOf(clue.key()).orElse(null)));
			} else {
				clues.forEach(this::add);
			}
		} catch (final RuntimeException failure) {
			cluesByKey.clear();
			cluesByKey.putAll(cluesBefore);
			locationsByKey.clear();
			locationsByKey.putAll(locationsBefore);
			throw failure;
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
		if (finder != null) {
			candidate = candidate.foundBy(finder);
		}
		Clue previous = cluesByKey.putIfAbsent(candidate.key(), candidate);

		/*
		 * Anonymous keys carry no semantics. In the very rare case of a random
		 * collision, preserve both observations by assigning the incoming clue
		 * a fresh key.
		 */
		while (previous != null && candidate.isAnonymous()) {
			candidate = candidate.rekeyAnonymous();
			previous = cluesByKey.putIfAbsent(candidate.key(), candidate);
		}

		if (previous != null) {
			throw new DuplicateClueException(previous, locationsByKey.get(previous.key()), candidate, location);
		}

		if (location != null) {
			locationsByKey.put(candidate.key(), location);
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
		return new Clues(Collections.unmodifiableMap(new LinkedHashMap<>(cluesByKey)),
				Collections.unmodifiableMap(new HashMap<>(locationsByKey)));
	}

}
