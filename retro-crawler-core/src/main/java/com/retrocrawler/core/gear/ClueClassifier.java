package com.retrocrawler.core.gear;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.ClueAccumulator;
import com.retrocrawler.core.archive.clues.Clues;
import com.retrocrawler.core.archive.clues.DuplicateClueException;
import com.retrocrawler.core.archive.clues.InternalClueKeys;

/**
 * Applies model-aware clue semantics during gear resolution without mutating
 * the raw artifact. Finders and cached archives can consequently remain
 * independent of the model vocabulary.
 */
final class ClueClassifier {

	private final Set<String> exactKnownKeys;

	private final Map<String, List<String>> knownKeysIgnoringCase;

	ClueClassifier(final Set<String> knownFactKeys) {
		Objects.requireNonNull(knownFactKeys, "knownFactKeys");

		final Set<String> exact = new LinkedHashSet<>();
		final Map<String, List<String>> ignoringCase = new HashMap<>();
		for (final String key : knownFactKeys) {
			final String nonNullKey = Objects.requireNonNull(key, "knownFactKeys must not contain null");
			if (isReserved(nonNullKey)) {
				continue;
			}
			exact.add(nonNullKey);
			ignoringCase.computeIfAbsent(normalize(nonNullKey), ignored -> new ArrayList<>()).add(nonNullKey);
		}
		this.exactKnownKeys = Set.copyOf(exact);
		this.knownKeysIgnoringCase = Map.copyOf(ignoringCase);
	}

	/**
	 * Classifying can give an anonymous observation a semantic key, so the
	 * result has to be accumulated again rather than carried over: that new key
	 * may collide with one an explicit clue already claims.
	 */
	Clues classify(final Clues clues) {
		Objects.requireNonNull(clues, "clues");

		final ClueAccumulator classified = Clues.accumulator();
		/*
		 * Classifying strips a reinterpreted anonymous observation down to a
		 * missing-value clue, so the accumulator can no longer show what the
		 * archive actually said. Remember the raw form to name both sides of a
		 * conflict.
		 */
		final Map<String, Clue> observedByKey = new HashMap<>();
		for (final Clue clue : clues) {
			classify(Objects.requireNonNull(clue, "clues must not contain null")).ifPresent(value -> {
				add(classified, observedByKey.get(value.key()), clue, value);
				observedByKey.put(value.key(), clue);
			});
		}
		return classified.clues();
	}

	/**
	 * The accumulator owns the one-authority rule; this only restates its
	 * rejection in terms the accumulator cannot see.
	 */
	private static void add(final ClueAccumulator classified, final Clue previous, final Clue observed,
			final Clue value) {
		try {
			classified.add(value);
		} catch (final DuplicateClueException e) {
			throw new DuplicateClueException("More than one clue claims semantic key '" + value.key() + "'. " + previous
					+ " competes with " + observed + ".", e);
		}
	}

	private Optional<Clue> classify(final Clue clue) {
		if (clue.value().isEmpty()) {
			return clue.isAnonymous() ? Optional.empty() : Optional.of(clue);
		}

		if (clue.value().stream().allMatch(String::isBlank)) {
			return clue.isAnonymous() ? Optional.empty() : Optional.of(Clue.missingValue(clue.key()));
		}

		if (!clue.isAnonymous() || clue.value().size() != 1) {
			return Optional.of(clue);
		}

		final String candidate = clue.value().iterator().next().trim();
		final String knownKey = findKnownKey(candidate);
		return knownKey == null ? Optional.of(clue) : Optional.of(Clue.missingValue(knownKey));
	}

	private String findKnownKey(final String candidate) {
		if (exactKnownKeys.contains(candidate)) {
			return candidate;
		}
		final List<String> matches = knownKeysIgnoringCase.getOrDefault(normalize(candidate), List.of());
		return matches.size() == 1 ? matches.getFirst() : null;
	}

	private static boolean isReserved(final String key) {
		return key.startsWith(Clue.PREFIX_ANONYMOUS) || key.startsWith(InternalClueKeys.PREFIX);
	}

	private static String normalize(final String value) {
		return value.toLowerCase(Locale.ROOT);
	}
}
