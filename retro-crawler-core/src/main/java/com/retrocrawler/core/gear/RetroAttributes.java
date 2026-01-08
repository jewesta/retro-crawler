package com.retrocrawler.core.gear;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.util.RetroAttribute;

public class RetroAttributes {

	private final Map<String, String> anonymousToKnown = new HashMap<>();

	private final Map<String, RetroAttribute> attributes = new HashMap<>();

	public void put(final RetroAttribute attribute) {
		if (contains(attribute)) {
			throw new IllegalArgumentException("Already contains attribute with key '" + attribute.getKey() + "'.");
		}
		final String key = attribute.getKey();
		attributes.put(key, attribute);
		/*
		 * Clues can be turned from anonymous to known. Make a note of the initial
		 * anonymous key so we can identify former anonymous clues as already added as a
		 * fact.
		 */
		if (attribute instanceof final Fact fact) {
			final Clue source = fact.source();
			if (source.isAnonymous()) {
				anonymousToKnown.put(source.getKey(), key);
			}
		}
	}

	public RetroAttribute get(final String key) {
		return attributes.get(key);
	}

	public boolean containsKey(final String key) {
		return attributes.containsKey(key);
	}

	public boolean contains(final RetroAttribute attribute) {
		if (attribute instanceof final Clue clue) {
			return containsClue(clue);
		}
		if (attribute instanceof final Fact fact) {
			return containsFact(fact);
		}
		throw new IllegalArgumentException("Expected the attribute to be either a " + Clue.class.getSimpleName()
				+ " or a " + Fact.class.getSimpleName() + " but got " + attribute.getClass().getName() + ".");
	}

	public <T extends RetroAttribute> boolean is(final String key, final Class<T> type) {
		return type.isInstance(get(key));
	}

	public boolean isFact(final String key) {
		return is(key, Fact.class);
	}

	public boolean isClue(final String key) {
		return is(key, Clue.class);
	}

	public boolean containsFact(final Fact fact) {
		final RetroAttribute existing = attributes.get(fact.getKey());
		return existing instanceof Fact;
	}

	public boolean containsClue(final Clue clue) {
		final String key = clue.getKey();
		if (attributes.containsKey(key)) {
			return true;
		}
		return clue.isAnonymous() && anonymousToKnown.containsKey(key);
	}

	public Set<Fact> getFacts() {
		return attributes.values().stream().filter(Fact.class::isInstance).map(Fact.class::cast)
				.collect(Collectors.toUnmodifiableSet());
	}

	public Set<Clue> getClues() {
		return attributes.values().stream().filter(Clue.class::isInstance).map(Clue.class::cast)
				.collect(Collectors.toUnmodifiableSet());
	}

	public Map<String, RetroAttribute> getAll() {
		return new LinkedHashMap<>(attributes);
	}

}
