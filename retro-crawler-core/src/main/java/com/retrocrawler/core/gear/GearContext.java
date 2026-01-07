package com.retrocrawler.core.gear;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.retrocrawler.core.archive.clues.Artifact;
import com.retrocrawler.core.util.RetroAttribute;

public record GearContext(Class<?> gearType, Artifact artifact, RetroAttributes attributes) {

	public GearContext {
		Objects.requireNonNull(gearType, "gearType");
		Objects.requireNonNull(artifact, "artifact");
		Objects.requireNonNull(attributes, "attributes");
	}

	public Optional<Fact> getFact(final String key) {
		Objects.requireNonNull(key, "key");

		if (attributes.get(key) instanceof final Fact fact) {
			return Optional.of(fact);
		}
		return Optional.empty();
	}

	/**
	 * Convenience method for matchers that expect exactly one value.
	 * <p>
	 * If the fact contains multiple values or the value is not assignable to the
	 * requested type, {@link Optional#empty()} is returned.
	 */
	public <T> Optional<T> getFact(final String key, final Class<T> type) {
		Objects.requireNonNull(key, "key");
		Objects.requireNonNull(type, "type");

		return getFacts(key, type).filter(values -> values.size() == 1).map(values -> values.iterator().next());
	}

	/**
	 * Convenience method for matchers that work with multi-valued facts.
	 * <p>
	 * Returns {@link Optional#empty()} if no fact exists for the key or if any
	 * value is not assignable to the requested type.
	 */
	public <T> Optional<Set<T>> getFacts(final String key, final Class<T> type) {
		Objects.requireNonNull(key, "key");
		Objects.requireNonNull(type, "type");

		final RetroAttribute attribute = attributes.get(key);
		if (!(attribute instanceof final Fact fact)) {
			return Optional.empty();
		}

		final Set<Object> values = fact.getValue();
		if (values.isEmpty()) {
			// Should never happen by invariant, but be defensive.
			return Optional.empty();
		}

		// Ensure all values are compatible with T
		if (!values.stream().allMatch(type::isInstance)) {
			return Optional.empty();
		}

		@SuppressWarnings("unchecked")
		final Set<T> typedValues = values.stream().map(v -> (T) v).collect(Collectors.toUnmodifiableSet());

		return Optional.of(typedValues);
	}
}