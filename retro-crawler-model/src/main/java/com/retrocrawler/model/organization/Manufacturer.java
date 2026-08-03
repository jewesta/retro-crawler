package com.retrocrawler.model.organization;

import java.util.Objects;
import java.util.Optional;

import com.retrocrawler.model.identifier.TheRetroWebCategory;
import com.retrocrawler.model.identifier.TheRetroWebReference;

/**
 * A manufacturer identity with optional external reference enrichment supplied
 * by the selected catalog.
 *
 * @param name the usual short or trading name
 * @param fullName the full corporate name, when known
 * @param theRetroWebReference a linkable The Retro Web manufacturer reference,
 *        when supplied by the selected catalog
 */
public record Manufacturer(String name, Optional<String> fullName,
		Optional<TheRetroWebReference> theRetroWebReference) {

	public Manufacturer {
		name = requireName(name, "name");
		fullName = Objects.requireNonNull(fullName, "fullName")
					.map(value -> requireName(value, "fullName"));
		theRetroWebReference = Objects.requireNonNull(theRetroWebReference, "theRetroWebReference");
		theRetroWebReference.ifPresent(reference -> {
			if (reference.category() != TheRetroWebCategory.MANUFACTURER) {
				throw new IllegalArgumentException(
						"A manufacturer can only carry a The Retro Web manufacturer reference.");
			}
		});
	}

	public Manufacturer(final String name, final String fullName) {
		this(name, Optional.ofNullable(fullName), Optional.empty());
	}

	public Manufacturer(final String name, final String fullName,
			final TheRetroWebReference theRetroWebReference) {
		this(name, Optional.ofNullable(fullName), Optional.ofNullable(theRetroWebReference));
	}

	public Manufacturer(final String name) {
		this(name, Optional.empty(), Optional.empty());
	}

	@Override
	public String toString() {
		return name;
	}

	private static String requireName(final String value, final String field) {
		final String normalized = Objects.requireNonNull(value, field).trim();
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException("Manufacturer " + field + " must not be blank.");
		}
		return normalized;
	}
}
