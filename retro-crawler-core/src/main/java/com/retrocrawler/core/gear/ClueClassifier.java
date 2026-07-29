package com.retrocrawler.core.gear;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.retrocrawler.core.archive.clues.Clue;
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

	Set<Clue> classify(final Set<Clue> clues) {
		Objects.requireNonNull(clues, "clues");

		final Map<String, Clue> classified = new LinkedHashMap<>();
		for (final Clue clue : clues) {
			classify(Objects.requireNonNull(clue, "clues must not contain null"))
					.ifPresent(value -> merge(classified, value));
		}
		return Set.copyOf(classified.values());
	}

	private static void merge(final Map<String, Clue> cluesByKey, final Clue incoming) {
		final Clue previous = cluesByKey.get(incoming.getKey());
		if (previous == null) {
			cluesByKey.put(incoming.getKey(), incoming);
			return;
		}

		final Set<String> combinedValues = new LinkedHashSet<>(previous.getValue());
		combinedValues.addAll(incoming.getValue());
		cluesByKey.put(incoming.getKey(), Clue.of(incoming.getKey(), Set.copyOf(combinedValues)));
	}

	private Optional<Clue> classify(final Clue clue) {
		if (clue.getValue().isEmpty()) {
			return clue.isAnonymous() ? Optional.empty() : Optional.of(clue);
		}

		if (clue.getValue().stream().allMatch(String::isBlank)) {
			return clue.isAnonymous() ? Optional.empty() : Optional.of(Clue.missingValue(clue.getKey()));
		}

		if (!clue.isAnonymous() || clue.getValue().size() != 1) {
			return Optional.of(clue);
		}

		final String candidate = clue.getValue().iterator().next().trim();
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
