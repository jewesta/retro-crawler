package com.retrocrawler.model.identifier;

import static com.retrocrawler.model.identifier.NintendoGameBoyCartridgeCatalogKey.cartridge_code;
import static com.retrocrawler.model.identifier.NintendoGameBoyCartridgeCatalogKey.game_languages;
import static com.retrocrawler.model.identifier.NintendoGameBoyCartridgeCatalogKey.release_regions;
import static com.retrocrawler.model.identifier.NintendoGameBoyCartridgeCatalogKey.rom_id;
import static com.retrocrawler.model.identifier.NintendoGameBoyCartridgeCatalogKey.title;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.retrocrawler.core.catalog.Catalog;
import com.retrocrawler.core.catalog.Catalog.Row;
import com.retrocrawler.model.locale.LanguageCode;
import com.retrocrawler.model.locale.RegionCode;

/**
 * Typed indexes over a standard catalog of observed Nintendo Game Boy-family
 * cartridge label codes.
 *
 * <p>
 * The bundled catalog is not a validity authority. An absent code can be a new
 * observation, a typo, or a gap in the snapshot. The catalog only answers what
 * its dated sources currently know.
 */
public final class NintendoGameBoyCartridgeCatalog {

	static final String DEFAULT_CATALOG_FILE = "nintendo-game-boy-cartridge-catalog.tsv";

	private final Catalog<NintendoGameBoyCartridgeCatalogKey> catalog;
	private final List<NintendoGameBoyCartridgeCatalogEntry> entries;
	private final Map<NintendoGameBoyCartridgeCode, List<NintendoGameBoyCartridgeCatalogEntry>> byCode;
	private final Map<NintendoGameBoyRomId, List<NintendoGameBoyCartridgeCatalogEntry>> byRomId;
	private final Map<String, List<NintendoGameBoyCartridgeCatalogEntry>> byTitle;
	private final Map<RegionCode, List<NintendoGameBoyCartridgeCatalogEntry>> byReleaseRegion;
	private final Map<LanguageCode, List<NintendoGameBoyCartridgeCatalogEntry>> byGameLanguage;
	private final Set<NintendoGameBoyCartridgeCode> codes;

	private NintendoGameBoyCartridgeCatalog(final Catalog<NintendoGameBoyCartridgeCatalogKey> catalog) {
		this.catalog = Objects.requireNonNull(catalog, "catalog");
		final List<NintendoGameBoyCartridgeCatalogEntry> parsed = catalog.rows().stream()
				.map(NintendoGameBoyCartridgeCatalog::parseEntry).toList();
		entries = List.copyOf(new LinkedHashSet<>(parsed));
		byCode = immutableIndex(entries, NintendoGameBoyCartridgeCatalogEntry::cartridgeCode);
		byRomId = immutableIndex(entries, NintendoGameBoyCartridgeCatalogEntry::romId);
		byTitle = immutableIndex(entries, entry -> normalizeTitle(entry.title()));
		byReleaseRegion = immutableMultiIndex(entries, NintendoGameBoyCartridgeCatalogEntry::releaseRegions);
		byGameLanguage = immutableMultiIndex(entries, NintendoGameBoyCartridgeCatalogEntry::allGameLanguages);
		codes = Collections.unmodifiableSet(new LinkedHashSet<>(byCode.keySet()));
	}

	public static NintendoGameBoyCartridgeCatalog bundled() {
		return BundledHolder.INSTANCE;
	}

	/**
	 * Reads the same standard TSV format as the bundled resource. The caller
	 * retains ownership of the reader.
	 */
	public static NintendoGameBoyCartridgeCatalog read(final Reader source) {
		return from(Catalog.read(NintendoGameBoyCartridgeCatalogKey.class, source));
	}

	static NintendoGameBoyCartridgeCatalog from(final Catalog<NintendoGameBoyCartridgeCatalogKey> catalog) {
		return new NintendoGameBoyCartridgeCatalog(catalog);
	}

	Catalog<NintendoGameBoyCartridgeCatalogKey> catalog() {
		return catalog;
	}

	public List<NintendoGameBoyCartridgeCatalogEntry> entries() {
		return entries;
	}

	public Set<NintendoGameBoyCartridgeCode> codes() {
		return codes;
	}

	public boolean contains(final NintendoGameBoyCartridgeCode code) {
		return byCode.containsKey(Objects.requireNonNull(code, "code"));
	}

	public List<NintendoGameBoyCartridgeCatalogEntry> findByCode(final NintendoGameBoyCartridgeCode code) {
		return byCode.getOrDefault(Objects.requireNonNull(code, "code"), List.of());
	}

	public List<NintendoGameBoyCartridgeCatalogEntry> findByRomId(final NintendoGameBoyRomId romId) {
		return byRomId.getOrDefault(Objects.requireNonNull(romId, "romId"), List.of());
	}

	public List<NintendoGameBoyCartridgeCatalogEntry> findByTitle(final String query) {
		return byTitle.getOrDefault(normalizeTitle(query), List.of());
	}

	public List<NintendoGameBoyCartridgeCatalogEntry> findByReleaseRegion(final RegionCode region) {
		return byReleaseRegion.getOrDefault(Objects.requireNonNull(region, "region"), List.of());
	}

	public List<NintendoGameBoyCartridgeCatalogEntry> findByGameLanguage(final LanguageCode language) {
		return byGameLanguage.getOrDefault(Objects.requireNonNull(language, "language"), List.of());
	}

	/**
	 * Creates a catalog union, keeping the receiver's order and adding the
	 * supplement's rows. Duplicate typed entries are collapsed by this view.
	 */
	public NintendoGameBoyCartridgeCatalog plus(final NintendoGameBoyCartridgeCatalog supplement) {
		Objects.requireNonNull(supplement, "supplement");
		return from(catalog.plus(supplement.catalog));
	}

	private static NintendoGameBoyCartridgeCatalogEntry parseEntry(final Row<NintendoGameBoyCartridgeCatalogKey> row) {
		final int lineNumber = row.lineNumber();
		final NintendoGameBoyRomId romId = NintendoGameBoyRomId.parse(row.get(rom_id))
				.orElseThrow(() -> new IllegalArgumentException(
						"Invalid Game Boy ROM ID on catalog line " + lineNumber + ": " + row.get(rom_id)));
		return new NintendoGameBoyCartridgeCatalogEntry(new NintendoGameBoyCartridgeCode(row.get(cartridge_code)),
				romId, row.get(title), parseRegions(row.get(release_regions), lineNumber),
				parseGameLanguageSets(row.get(game_languages), lineNumber));
	}

	private static Set<RegionCode> parseRegions(final String value, final int lineNumber) {
		final Set<RegionCode> regions = new LinkedHashSet<>();
		for (final String code : value.split(",", -1)) {
			try {
				regions.add(new RegionCode(code));
			} catch (final IllegalArgumentException e) {
				throw new IllegalArgumentException("Invalid release region on catalog line " + lineNumber + ": " + code,
						e);
			}
		}
		return regions;
	}

	private static List<Set<LanguageCode>> parseGameLanguageSets(final String value, final int lineNumber) {
		if (value.isEmpty()) {
			return List.of();
		}
		final List<Set<LanguageCode>> games = new ArrayList<>();
		for (final String game : value.split("\\+", -1)) {
			final Set<LanguageCode> languages = new LinkedHashSet<>();
			for (final String code : game.split(",", -1)) {
				try {
					languages.add(new LanguageCode(code));
				} catch (final IllegalArgumentException e) {
					throw new IllegalArgumentException(
							"Invalid game language on catalog line " + lineNumber + ": " + code, e);
				}
			}
			games.add(languages);
		}
		return games;
	}

	private static String normalizeTitle(final String value) {
		final String normalized = Objects.requireNonNull(value, "value").trim().toLowerCase(Locale.ROOT);
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException("A catalog title lookup must not be blank.");
		}
		return normalized;
	}

	private static <K> Map<K, List<NintendoGameBoyCartridgeCatalogEntry>> immutableIndex(
			final List<NintendoGameBoyCartridgeCatalogEntry> entries,
			final java.util.function.Function<NintendoGameBoyCartridgeCatalogEntry, K> keyFunction) {
		final Map<K, List<NintendoGameBoyCartridgeCatalogEntry>> mutable = new LinkedHashMap<>();
		for (final NintendoGameBoyCartridgeCatalogEntry entry : entries) {
			mutable.computeIfAbsent(keyFunction.apply(entry), ignored -> new ArrayList<>()).add(entry);
		}
		final Map<K, List<NintendoGameBoyCartridgeCatalogEntry>> immutable = new LinkedHashMap<>();
		mutable.forEach((key, value) -> immutable.put(key, List.copyOf(value)));
		return Collections.unmodifiableMap(immutable);
	}

	private static <K> Map<K, List<NintendoGameBoyCartridgeCatalogEntry>> immutableMultiIndex(
			final List<NintendoGameBoyCartridgeCatalogEntry> entries,
			final java.util.function.Function<NintendoGameBoyCartridgeCatalogEntry, Set<K>> keyFunction) {
		final Map<K, List<NintendoGameBoyCartridgeCatalogEntry>> mutable = new LinkedHashMap<>();
		for (final NintendoGameBoyCartridgeCatalogEntry entry : entries) {
			for (final K key : keyFunction.apply(entry)) {
				mutable.computeIfAbsent(key, ignored -> new ArrayList<>()).add(entry);
			}
		}
		final Map<K, List<NintendoGameBoyCartridgeCatalogEntry>> immutable = new LinkedHashMap<>();
		mutable.forEach((key, value) -> immutable.put(key, List.copyOf(value)));
		return Collections.unmodifiableMap(immutable);
	}

	private static final class BundledHolder {

		private static final NintendoGameBoyCartridgeCatalog INSTANCE = load();

		private static NintendoGameBoyCartridgeCatalog load() {
			try (InputStream input = NintendoGameBoyCartridgeCatalog.class.getResourceAsStream(DEFAULT_CATALOG_FILE)) {
				if (input == null) {
					throw new IllegalStateException(
							"Missing bundled Game Boy cartridge catalog: " + DEFAULT_CATALOG_FILE);
				}
				return read(new InputStreamReader(input, StandardCharsets.UTF_8));
			} catch (final IOException e) {
				throw new UncheckedIOException("Could not close the bundled Game Boy cartridge catalog.", e);
			}
		}
	}
}
