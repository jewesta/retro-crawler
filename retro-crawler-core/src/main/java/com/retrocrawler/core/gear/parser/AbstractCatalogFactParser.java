package com.retrocrawler.core.gear.parser;

import java.util.Objects;

import com.retrocrawler.core.catalog.Catalog;
import com.retrocrawler.core.catalog.CatalogLoader;

/**
 * Standard immutable catalog loading for catalog-backed fact parsers.
 */
public abstract class AbstractCatalogFactParser<K extends Enum<K>, T> implements CatalogFactParser<K, T> {

	private final Catalog<K> catalog;

	protected AbstractCatalogFactParser(final CatalogLoader catalogs, final Class<K> keyType,
			final String defaultCatalogFile) {
		this(Objects.requireNonNull(catalogs, "catalogs").load(Objects.requireNonNull(keyType, "keyType"),
				requireFileName(defaultCatalogFile)));
	}

	protected AbstractCatalogFactParser(final Catalog<K> catalog) {
		this.catalog = Objects.requireNonNull(catalog, "catalog");
	}

	@Override
	public final Catalog<K> catalog() {
		return catalog;
	}

	private static String requireFileName(final String defaultCatalogFile) {
		Objects.requireNonNull(defaultCatalogFile, "defaultCatalogFile");
		if (defaultCatalogFile.isBlank()) {
			throw new IllegalArgumentException("defaultCatalogFile must not be blank.");
		}
		return defaultCatalogFile;
	}
}
