package com.retrocrawler.core.archive.clues;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.retrocrawler.core.archive.ARI;

/**
 * A model-independent bundle of {@link Clue clues} observed at one archive
 * location.
 * <p>
 * Artifacts form the rebuildable repository representation. They contain raw
 * evidence with its finder and explicitly declared source ARIs, never resolved
 * facts or gear. Resolution may derive an effective clue view but must leave
 * the artifact unchanged so that another model can reinterpret the same cached
 * evidence.
 * <p>
 * An artifact holds finished {@link Clues}, which already hold one clue per
 * key. It therefore inspects nothing on the way in: the clues were checked
 * where they were observed.
 */
public class Artifact {

	private static final String JSON_VALUE = "value";
	private static final String JSON_FINDER = "finder";
	private static final String JSON_SOURCES = "sources";
	private static final Set<String> JSON_PROPERTIES = Set.of(JSON_VALUE, JSON_FINDER, JSON_SOURCES);

	/*
	 * Not final because Jackson populates an artifact field by field through
	 * jsonSetter(...). Every value this field ever holds is nevertheless a
	 * complete, immutable, already-checked Clues rather than a builder the
	 * artifact would keep for its whole life.
	 */
	@JsonIgnore
	private Clues clues;

	/**
	 * {@link JsonAnySetter} is not compatible with constructor injection via
	 * {@link JsonCreator}. That is why we need this no-arg.
	 */
	protected Artifact() {
		// Jackson
		this.clues = Clues.none();
	}

	public Artifact(final Clues clues) {
		Objects.requireNonNull(clues,
				"Clues cannot be null. The existence of an artifact implies that there is at least one clue.");
		if (clues.isEmpty()) {
			throw new IllegalArgumentException(
					"Clues cannot be empty. The existence of an artifact implies that there is at least one clue.");
		}
		/*
		 * An artifact is the cache boundary, so crawl-time positions stop here.
		 * Although a retrieved clue retains source ARIs, it retains no snapshot
		 * of the text that was parsed. An old offset could therefore point into
		 * changed content, and diagnostics that differed depending on whether
		 * an archive came from a crawl or from the repository would be worse
		 * than none.
		 */
		this.clues = clues.withoutLocations();
	}

	/**
	 * Returns the raw clues, in observation order, one per key.
	 */
	public Clues clues() {
		return clues;
	}

	@JsonAnyGetter
	protected Map<String, Object> jsonGetter() {
		final Map<String, Object> storedClues = new LinkedHashMap<>();
		clues.forEach(clue -> {
			final Map<String, Object> storedClue = new LinkedHashMap<>();
			storedClue.put(JSON_VALUE, storedValue(clue));
			clue.finder().ifPresent(finder -> storedClue.put(JSON_FINDER, finder));
			if (!clue.sources().isEmpty()) {
				storedClue.put(JSON_SOURCES, clue.sources().stream().map(ARI::toString).toList());
			}
			storedClues.put(clue.key(), Collections.unmodifiableMap(storedClue));
		});
		return Collections.unmodifiableMap(storedClues);
	}

	@JsonAnySetter
	protected void jsonSetter(final String key, final Object value) {
		if (!(value instanceof final Map<?, ?> stored)) {
			throw new IllegalArgumentException("Expected stored clue '" + key + "' to be an object but got: "
					+ (value == null ? "null" : value.getClass().getName()));
		}
		for (final Object property : stored.keySet()) {
			if (!(property instanceof final String name) || !JSON_PROPERTIES.contains(name)) {
				throw new IllegalArgumentException("Unexpected property on stored clue '" + key + "': " + property);
			}
		}
		if (!stored.containsKey(JSON_VALUE)) {
			throw new IllegalArgumentException("Stored clue '" + key + "' has no value.");
		}

		final Set<String> values = storedValues(key, stored.get(JSON_VALUE));
		final String finder = storedFinder(key, stored.get(JSON_FINDER));
		final List<ARI> sources = storedSources(key, stored.get(JSON_SOURCES));
		final Clue clue = new Clue(key, values, finder, sources);
		clues = clues.and(clue);
	}

	private static Object storedValue(final Clue clue) {
		return switch (clue.size()) {
		case 0 -> List.of();
		case 1 -> clue.value().iterator().next();
		default -> clue.value();
		};
	}

	private static Set<String> storedValues(final String key, final Object value) {
		if (value instanceof final String string) {
			return Set.of(string);
		}
		if (value instanceof final List<?> list) {
			final Set<String> values = new LinkedHashSet<>();
			for (final Object entry : list) {
				if (!(entry instanceof final String string)) {
					throw new IllegalArgumentException(
							"Stored clue '" + key + "' contains a non-string value: " + entry);
				}
				values.add(string);
			}
			return values;
		}
		throw new IllegalArgumentException("Expected stored clue '" + key + "' value to be a string or array but got: "
				+ (value == null ? "null" : value.getClass().getName()));
	}

	private static String storedFinder(final String key, final Object value) {
		if (value == null) {
			return null;
		}
		if (!(value instanceof final String finder)) {
			throw new IllegalArgumentException(
					"Expected stored clue '" + key + "' finder to be a string but got: " + value.getClass().getName());
		}
		return finder;
	}

	private static List<ARI> storedSources(final String key, final Object value) {
		if (value == null) {
			return List.of();
		}
		if (!(value instanceof final List<?> list)) {
			throw new IllegalArgumentException(
					"Expected stored clue '" + key + "' sources to be an array but got: " + value.getClass().getName());
		}
		final List<ARI> sources = new ArrayList<>();
		for (final Object entry : list) {
			if (!(entry instanceof final String source)) {
				throw new IllegalArgumentException("Stored clue '" + key + "' contains a non-string source: " + entry);
			}
			sources.add(ARI.parse(source));
		}
		return sources;
	}

	@Override
	public String toString() {
		return clues.toString();
	}

}
