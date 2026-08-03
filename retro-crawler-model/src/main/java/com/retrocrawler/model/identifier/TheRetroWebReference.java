package com.retrocrawler.model.identifier;

import java.net.URI;
import java.util.Objects;

/**
 * An unambiguous reference to an entry in The Retro Web.
 *
 * <p>
 * The numeric lookup route is commonly used and redirects to the entry's
 * canonical slug, but is not a documented public API. Constructing the URI
 * performs no network access and does not resolve that redirect.
 */
public record TheRetroWebReference(TheRetroWebCategory category, TheRetroWebId id) {

	private static final String BASE_URL = "https://theretroweb.com/";

	public TheRetroWebReference {
		Objects.requireNonNull(category, "category");
		Objects.requireNonNull(id, "id");
	}

	public URI lookupUri() {
		return URI.create(BASE_URL + category.path() + "/" + id.value());
	}
}
