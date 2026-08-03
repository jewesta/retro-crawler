package com.retrocrawler.core.catalog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * An immutable, strictly schematized TSV catalog. Enum constant names are the
 * complete set of allowed and required column keys.
 */
public final class Catalog<K extends Enum<K>> {

	private final Class<K> keyType;
	private final List<Row<K>> rows;

	private Catalog(final Class<K> keyType, final List<Row<K>> rows) {
		this.keyType = Objects.requireNonNull(keyType, "keyType");
		this.rows = List.copyOf(rows);
	}

	/**
	 * Reads a catalog while leaving ownership of the supplied reader with the
	 * caller.
	 */
	public static <K extends Enum<K>> Catalog<K> read(final Class<K> keyType, final Reader source) {
		Objects.requireNonNull(keyType, "keyType");
		Objects.requireNonNull(source, "source");
		final K[] constants = keyType.getEnumConstants();
		if (constants == null || constants.length == 0) {
			throw new IllegalArgumentException("Catalog key type must declare at least one enum constant: "
					+ keyType.getName());
		}

		final Map<String, K> keysByName = new LinkedHashMap<>();
		for (final K key : constants) {
			keysByName.put(key.name(), key);
		}

		final BufferedReader reader = source instanceof final BufferedReader buffered
				? buffered
				: new BufferedReader(source);
		List<K> header = null;
		final List<Row<K>> rows = new ArrayList<>();
		int lineNumber = 0;
		try {
			String line;
			while ((line = reader.readLine()) != null) {
				lineNumber++;
				if (line.isBlank() || line.stripLeading().startsWith("#")) {
					continue;
				}
				if (header == null) {
					header = parseHeader(keyType, keysByName, removeBom(line), lineNumber);
				} else {
					rows.add(parseRow(keyType, header, line, lineNumber));
				}
			}
		} catch (final IOException e) {
			throw new UncheckedIOException("Could not read catalog for key type " + keyType.getName() + ".", e);
		}

		if (header == null) {
			throw new IllegalArgumentException("Catalog for key type " + keyType.getName() + " has no header.");
		}
		return new Catalog<>(keyType, rows);
	}

	public Class<K> keyType() {
		return keyType;
	}

	public List<Row<K>> rows() {
		return rows;
	}

	/**
	 * Returns the row-wise union of this catalog and a compatible supplement.
	 * Rows remain in source order.
	 */
	public Catalog<K> plus(final Catalog<K> supplement) {
		Objects.requireNonNull(supplement, "supplement");
		if (supplement.keyType != keyType) {
			throw new IllegalArgumentException("Cannot combine catalogs with key types " + keyType.getName()
					+ " and " + supplement.keyType.getName() + ".");
		}
		final List<Row<K>> combined = new ArrayList<>(rows);
		combined.addAll(supplement.rows);
		return new Catalog<>(keyType, combined);
	}

	private static String removeBom(final String line) {
		return !line.isEmpty() && line.charAt(0) == '\ufeff' ? line.substring(1) : line;
	}

	private static <K extends Enum<K>> List<K> parseHeader(final Class<K> keyType,
			final Map<String, K> keysByName, final String line, final int lineNumber) {
		final String[] cells = line.split("\t", -1);
		final List<K> header = new ArrayList<>(cells.length);
		final Set<K> found = new LinkedHashSet<>();
		for (final String cell : cells) {
			final K key = keysByName.get(cell);
			if (key == null) {
				throw new IllegalArgumentException("Unknown catalog header key '" + cell + "' on line "
						+ lineNumber + " for " + keyType.getName() + ".");
			}
			if (!found.add(key)) {
				throw new IllegalArgumentException("Duplicate catalog header key '" + cell + "' on line "
						+ lineNumber + " for " + keyType.getName() + ".");
			}
			header.add(key);
		}

		final Set<K> missing = new LinkedHashSet<>(keysByName.values());
		missing.removeAll(found);
		if (!missing.isEmpty()) {
			throw new IllegalArgumentException("Missing catalog header keys "
					+ missing.stream().map(Enum::name).toList() + " on line " + lineNumber + " for "
					+ keyType.getName() + ".");
		}
		return List.copyOf(header);
	}

	private static <K extends Enum<K>> Row<K> parseRow(final Class<K> keyType, final List<K> header,
			final String line, final int lineNumber) {
		final String[] cells = line.split("\t", -1);
		if (cells.length != header.size()) {
			throw new IllegalArgumentException("Expected " + header.size() + " catalog columns but found "
					+ cells.length + " on line " + lineNumber + " for " + keyType.getName() + ".");
		}
		final Map<K, String> values = new EnumMap<>(keyType);
		for (int index = 0; index < header.size(); index++) {
			values.put(header.get(index), cells[index]);
		}
		return new Row<>(keyType, lineNumber, values);
	}

	/**
	 * One immutable catalog row addressed by schema key rather than column order.
	 */
	public static final class Row<K extends Enum<K>> {

		private final Class<K> keyType;
		private final int lineNumber;
		private final Map<K, String> values;

		private Row(final Class<K> keyType, final int lineNumber, final Map<K, String> values) {
			this.keyType = keyType;
			this.lineNumber = lineNumber;
			this.values = Collections.unmodifiableMap(new EnumMap<>(values));
		}

		public String get(final K key) {
			Objects.requireNonNull(key, "key");
			if (key.getDeclaringClass() != keyType) {
				throw new IllegalArgumentException("Expected catalog key of type " + keyType.getName() + ".");
			}
			return values.get(key);
		}

		public int lineNumber() {
			return lineNumber;
		}

		public Map<K, String> values() {
			return values;
		}
	}
}
