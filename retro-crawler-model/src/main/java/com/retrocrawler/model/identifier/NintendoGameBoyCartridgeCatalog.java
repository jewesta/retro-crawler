package com.retrocrawler.model.identifier;

import java.io.BufferedReader;
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

import com.retrocrawler.model.locale.LanguageCode;
import com.retrocrawler.model.locale.RegionCode;

/**
 * A resource-backed snapshot of observed Nintendo Game Boy-family cartridge
 * label codes.
 *
 * <p>
 * The bundled catalogue is not a validity authority. An absent code can be a
 * new observation, a typo, or a gap in the snapshot. The catalogue only
 * answers what its dated sources currently know.
 */
public final class NintendoGameBoyCartridgeCatalog {

	private static final String RESOURCE =
			"/com/retrocrawler/model/identifier/nintendo-game-boy-cartridge-catalog.tsv";
	private static final String HEADER =
			"cartridge_code\trom_id\ttitle\trelease_regions\tgame_languages";

	private final List<NintendoGameBoyCartridgeCatalogEntry> entries;
	private final Map<NintendoGameBoyCartridgeCode, List<NintendoGameBoyCartridgeCatalogEntry>> byCode;
	private final Map<NintendoGameBoyRomId, List<NintendoGameBoyCartridgeCatalogEntry>> byRomId;
	private final Map<String, List<NintendoGameBoyCartridgeCatalogEntry>> byTitle;
	private final Map<RegionCode, List<NintendoGameBoyCartridgeCatalogEntry>> byReleaseRegion;
	private final Map<LanguageCode, List<NintendoGameBoyCartridgeCatalogEntry>> byGameLanguage;
	private final Set<NintendoGameBoyCartridgeCode> codes;

	private NintendoGameBoyCartridgeCatalog(final List<NintendoGameBoyCartridgeCatalogEntry> sourceEntries) {
		entries = List.copyOf(new LinkedHashSet<>(sourceEntries));
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
	 * Reads the same tab-separated format as the bundled resource. The caller
	 * retains ownership of the reader.
	 */
	public static NintendoGameBoyCartridgeCatalog read(final Reader source) {
		Objects.requireNonNull(source, "source");
		final BufferedReader reader = source instanceof final BufferedReader buffered
				? buffered
				: new BufferedReader(source);
		final List<NintendoGameBoyCartridgeCatalogEntry> entries = new ArrayList<>();
		String line;
		boolean headerSeen = false;
		int lineNumber = 0;
		try {
			while ((line = reader.readLine()) != null) {
				lineNumber++;
				if (line.isBlank() || line.startsWith("#")) {
					continue;
				}
				if (!headerSeen) {
					if (!HEADER.equals(line)) {
						throw new IllegalArgumentException("Unexpected cartridge catalogue header on line "
								+ lineNumber + ": " + line);
					}
					headerSeen = true;
					continue;
				}
				entries.add(parseEntry(line, lineNumber));
			}
		} catch (final IOException e) {
			throw new UncheckedIOException("Could not read the Game Boy cartridge catalogue.", e);
		}

		if (!headerSeen) {
			throw new IllegalArgumentException("The cartridge catalogue has no header.");
		}
		return new NintendoGameBoyCartridgeCatalog(entries);
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

	public List<NintendoGameBoyCartridgeCatalogEntry> findByTitle(final String title) {
		return byTitle.getOrDefault(normalizeTitle(title), List.of());
	}

	public List<NintendoGameBoyCartridgeCatalogEntry> findByReleaseRegion(final RegionCode region) {
		return byReleaseRegion.getOrDefault(Objects.requireNonNull(region, "region"), List.of());
	}

	public List<NintendoGameBoyCartridgeCatalogEntry> findByGameLanguage(final LanguageCode language) {
		return byGameLanguage.getOrDefault(Objects.requireNonNull(language, "language"), List.of());
	}

	/**
	 * Creates a catalogue union, keeping the receiver's order and adding only new
	 * associations from the supplement.
	 */
	public NintendoGameBoyCartridgeCatalog plus(final NintendoGameBoyCartridgeCatalog supplement) {
		Objects.requireNonNull(supplement, "supplement");
		final List<NintendoGameBoyCartridgeCatalogEntry> combined = new ArrayList<>(entries);
		combined.addAll(supplement.entries);
		return new NintendoGameBoyCartridgeCatalog(combined);
	}

	private static NintendoGameBoyCartridgeCatalogEntry parseEntry(final String line, final int lineNumber) {
		final String[] columns = line.split("\t", -1);
		if (columns.length != 5) {
			throw new IllegalArgumentException(
					"Expected five cartridge catalogue columns on line " + lineNumber + ".");
		}
		final NintendoGameBoyRomId romId = NintendoGameBoyRomId.parse(columns[1])
				.orElseThrow(() -> new IllegalArgumentException(
						"Invalid Game Boy ROM ID on catalogue line " + lineNumber + ": " + columns[1]));
		return new NintendoGameBoyCartridgeCatalogEntry(
				new NintendoGameBoyCartridgeCode(columns[0]), romId, columns[2],
				parseRegions(columns[3], lineNumber), parseGameLanguageSets(columns[4], lineNumber));
	}

	private static Set<RegionCode> parseRegions(final String value, final int lineNumber) {
		final Set<RegionCode> regions = new LinkedHashSet<>();
		for (final String code : value.split(",", -1)) {
			try {
				regions.add(new RegionCode(code));
			} catch (final IllegalArgumentException e) {
				throw new IllegalArgumentException(
						"Invalid release region on catalogue line " + lineNumber + ": " + code, e);
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
							"Invalid game language on catalogue line " + lineNumber + ": " + code, e);
				}
			}
			games.add(languages);
		}
		return games;
	}

	private static String normalizeTitle(final String title) {
		final String normalized = Objects.requireNonNull(title, "title").trim().toLowerCase(Locale.ROOT);
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException("A catalogue title lookup must not be blank.");
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
			try (InputStream input = NintendoGameBoyCartridgeCatalog.class.getResourceAsStream(RESOURCE)) {
				if (input == null) {
					throw new IllegalStateException("Missing bundled Game Boy cartridge catalogue: " + RESOURCE);
				}
				return read(new InputStreamReader(input, StandardCharsets.UTF_8));
			} catch (final IOException e) {
				throw new UncheckedIOException("Could not close the bundled Game Boy cartridge catalogue.", e);
			}
		}
	}
}
