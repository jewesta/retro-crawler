package com.retrocrawler.model.organization;

import static com.retrocrawler.model.organization.ManufacturerCatalogKey.full_name;
import static com.retrocrawler.model.organization.ManufacturerCatalogKey.name;
import static com.retrocrawler.model.organization.ManufacturerCatalogKey.trw_id;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.retrocrawler.core.catalog.Catalog;
import com.retrocrawler.core.catalog.Catalog.Row;
import com.retrocrawler.model.identifier.TheRetroWebCategory;
import com.retrocrawler.model.identifier.TheRetroWebId;
import com.retrocrawler.model.identifier.TheRetroWebReference;

/**
 * Typed manufacturer identities and exact name lookups over a standard
 * catalog.
 *
 * <p>
 * A name can deliberately resolve to more than one manufacturer. Consumers
 * must not silently choose between ambiguous catalog entries.
 */
public final class ManufacturerCatalog {

	static final String DEFAULT_CATALOG_FILE = "manufacturer-catalog.tsv";

	private final Catalog<ManufacturerCatalogKey> catalog;
	private final List<Manufacturer> entries;
	private final Map<String, List<Manufacturer>> byName;

	private ManufacturerCatalog(final Catalog<ManufacturerCatalogKey> catalog) {
		this.catalog = Objects.requireNonNull(catalog, "catalog");
		entries = List.copyOf(new LinkedHashSet<>(catalog.rows().stream()
				.map(ManufacturerCatalog::parseEntry).toList()));
		byName = indexNames(entries);
	}

	public static ManufacturerCatalog bundled() {
		return BundledHolder.INSTANCE;
	}

	/**
	 * Reads the standard TSV format while leaving ownership of the supplied
	 * reader with the caller.
	 */
	public static ManufacturerCatalog read(final Reader source) {
		return from(Catalog.read(ManufacturerCatalogKey.class, source));
	}

	static ManufacturerCatalog from(final Catalog<ManufacturerCatalogKey> catalog) {
		return new ManufacturerCatalog(catalog);
	}

	Catalog<ManufacturerCatalogKey> catalog() {
		return catalog;
	}

	public List<Manufacturer> entries() {
		return entries;
	}

	/**
	 * Finds manufacturers whose short or full name exactly matches the query,
	 * ignoring case and surrounding whitespace.
	 */
	public List<Manufacturer> findByName(final String query) {
		return byName.getOrDefault(normalizeName(query), List.of());
	}

	private static Manufacturer parseEntry(final Row<ManufacturerCatalogKey> row) {
		final String fullName = row.get(full_name);
		return new Manufacturer(row.get(name), fullName.isBlank() ? Optional.empty() : Optional.of(fullName),
				parseTheRetroWebReference(row));
	}

	private static Optional<TheRetroWebReference> parseTheRetroWebReference(
			final Row<ManufacturerCatalogKey> row) {
		final String rawId = row.get(trw_id).trim();
		if (rawId.isEmpty()) {
			return Optional.empty();
		}
		try {
			return Optional.of(TheRetroWebCategory.MANUFACTURER.reference(
					new TheRetroWebId(Integer.parseInt(rawId))));
		} catch (final IllegalArgumentException e) {
			throw new IllegalArgumentException(
					"Invalid The Retro Web manufacturer ID on catalog line " + row.lineNumber() + ": " + rawId, e);
		}
	}

	private static Map<String, List<Manufacturer>> indexNames(final List<Manufacturer> manufacturers) {
		final Map<String, LinkedHashSet<Manufacturer>> mutable = new LinkedHashMap<>();
		for (final Manufacturer manufacturer : manufacturers) {
			mutable.computeIfAbsent(normalizeName(manufacturer.name()), ignored -> new LinkedHashSet<>())
					.add(manufacturer);
			manufacturer.fullName().ifPresent(value -> mutable
					.computeIfAbsent(normalizeName(value), ignored -> new LinkedHashSet<>())
					.add(manufacturer));
		}

		final Map<String, List<Manufacturer>> immutable = new LinkedHashMap<>();
		mutable.forEach((key, value) -> immutable.put(key, List.copyOf(value)));
		return Collections.unmodifiableMap(immutable);
	}

	private static String normalizeName(final String value) {
		final String normalized = Objects.requireNonNull(value, "value").trim().toLowerCase(Locale.ROOT);
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException("A manufacturer catalog lookup must not be blank.");
		}
		return normalized;
	}

	private static final class BundledHolder {

		private static final ManufacturerCatalog INSTANCE = load();

		private static ManufacturerCatalog load() {
			try (InputStream input = ManufacturerCatalog.class.getResourceAsStream(DEFAULT_CATALOG_FILE)) {
				if (input == null) {
					throw new IllegalStateException("Missing bundled manufacturer catalog: "
							+ DEFAULT_CATALOG_FILE);
				}
				return read(new InputStreamReader(input, StandardCharsets.UTF_8));
			} catch (final IOException e) {
				throw new UncheckedIOException("Could not close the bundled manufacturer catalog.", e);
			}
		}
	}
}
