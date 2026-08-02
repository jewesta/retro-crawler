package com.retrocrawler.model.identifier;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.retrocrawler.model.locale.LanguageCode;
import com.retrocrawler.model.locale.RegionCode;

/**
 * One observed association between a printed cartridge code, a ROM ID, a
 * title, its release markets, and any game languages explicitly stated by the
 * source. A cartridge code can have more than one such association.
 *
 * <p>
 * The outer {@code gameLanguageSets} list preserves the boundary between games
 * on compilation cartridges. An empty list means that the source did not state
 * language information; it does not mean that the game has no language.
 */
public record NintendoGameBoyCartridgeCatalogEntry(NintendoGameBoyCartridgeCode cartridgeCode,
		NintendoGameBoyRomId romId, String title, Set<RegionCode> releaseRegions,
		List<Set<LanguageCode>> gameLanguageSets) {

	public NintendoGameBoyCartridgeCatalogEntry {
		Objects.requireNonNull(cartridgeCode, "cartridgeCode");
		Objects.requireNonNull(romId, "romId");
		title = Objects.requireNonNull(title, "title").trim();
		if (title.isEmpty()) {
			throw new IllegalArgumentException("A cartridge catalogue title must not be blank.");
		}
		releaseRegions = immutableSet(releaseRegions, "releaseRegions");
		if (releaseRegions.isEmpty()) {
			throw new IllegalArgumentException("A cartridge catalogue entry must have a release region.");
		}
		gameLanguageSets = Objects.requireNonNull(gameLanguageSets, "gameLanguageSets").stream()
				.map(languages -> {
					final Set<LanguageCode> immutable = immutableSet(languages, "gameLanguageSets element");
					if (immutable.isEmpty()) {
						throw new IllegalArgumentException("A stated game-language set must not be empty.");
					}
					return immutable;
				})
				.toList();
	}

	/**
	 * Returns the union of all explicitly stated language sets.
	 */
	public Set<LanguageCode> allGameLanguages() {
		final Set<LanguageCode> languages = new LinkedHashSet<>();
		gameLanguageSets.forEach(languages::addAll);
		return Set.copyOf(languages);
	}

	private static <T> Set<T> immutableSet(final Set<T> values, final String name) {
		return Set.copyOf(Objects.requireNonNull(values, name));
	}
}
