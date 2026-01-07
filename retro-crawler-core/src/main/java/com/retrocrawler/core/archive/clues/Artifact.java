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

public class Artifact {

	@JsonIgnore
	private final Set<Clue> clues;

	/**
	 * {@link JsonAnySetter} is not compatible with constructor injection via
	 * {@link JsonCreator}. That is why we need this no-arg.
	 */
	protected Artifact() {
		// Jackson
		this.clues = new HashSet<>();
	}

	public Artifact(final Set<Clue> clues) {
		Objects.requireNonNull(clues,
				"Clues cannot be null. The existence of an artifact implies that there is at least one clue.");
		if (clues.isEmpty()) {
			throw new IllegalArgumentException(
					"Clues cannot be empty. The existence of an artifact implies that there is at least one clue.");
		}
		this.clues = clues;
	}

	public Set<Clue> getClues() {
		return clues;
	}

	@JsonAnyGetter
	protected Map<String, Object> jsonGetter() {
		return clues.stream().collect(Collectors.toUnmodifiableMap(Clue::getKey, clue -> {
			switch (clue.size()) {
			case 0:
				return null;
			case 1:
				/*
				 * This makes the JSON a bit smaller and less verbose.
				 */
				return clue.getValue().iterator().next();
			default:
				return clue.getValue();
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
		clues.add(clue);
	}

	@Override
	public String toString() {
		return clues.toString();
	}

}
