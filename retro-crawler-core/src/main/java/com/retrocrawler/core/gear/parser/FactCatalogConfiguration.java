package com.retrocrawler.core.gear.parser;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable collection-specific configuration for a catalog-backed fact parser.
 */
public final class FactCatalogConfiguration {

	private final String catalogFile;

	private FactCatalogConfiguration(final String catalogFile) {
		this.catalogFile = catalogFile;
	}

	public Optional<String> catalogFile() {
		return Optional.ofNullable(catalogFile);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {

		private String catalogFile;

		public Builder catalogFile(final String value) {
			Objects.requireNonNull(value, "value");
			final String normalized = value.trim();
			if (normalized.isEmpty()) {
				throw new IllegalArgumentException("catalogFile must not be blank.");
			}
			catalogFile = normalized;
			return this;
		}

		public FactCatalogConfiguration build() {
			if (catalogFile == null) {
				throw new IllegalStateException("At least one fact catalog setting is required.");
			}
			return new FactCatalogConfiguration(catalogFile);
		}
	}
}
