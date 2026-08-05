package com.retrocrawler.core.archive.clues;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A model-independent bundle of {@link Clue clues} observed at one archive
 * location.
 * <p>
 * Artifacts form the rebuildable repository representation. They contain raw
 * evidence, never resolved facts or gear. Resolution may derive an effective
 * clue view but must leave the artifact unchanged so that another model can
 * reinterpret the same cached evidence.
 * <p>
 * An artifact holds finished {@link Clues}, which already hold one clue per key.
 * It therefore inspects nothing on the way in: the clues were checked where they
 * were observed.
 */
public class Artifact {

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
		 * A retrieved artifact has no folder name or document left to point
		 * into, and diagnostics that differed depending on whether an archive
		 * came from a crawl or from the repository would be worse than none.
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
		return clues.stream().collect(Collectors.toUnmodifiableMap(Clue::key, clue -> {
			switch (clue.size()) {
			case 0:
				return List.of();
			case 1:
				/*
				 * This makes the JSON a bit smaller and less verbose.
				 */
				return clue.value().iterator().next();
			default:
				return clue.value();
			}
		}));
	}

	@JsonAnySetter
	protected void jsonSetter(final String key, final Object value) {
		@SuppressWarnings("unchecked")
		final Clue clue = switch (value) {
		case final List<?> values -> new Clue(key, new HashSet<>((List<String>) values));
		case final String string -> new Clue(key, Set.of(string));
		case null -> throw new AssertionError("Expected to find either a String or a List<String> but got: null");
		default -> throw new AssertionError(
				"Expected to find either a String or a List<String> but got: " + value.getClass().getName());
		};
		clues = clues.and(clue);
	}

	@Override
	public String toString() {
		return clues.toString();
	}

}
