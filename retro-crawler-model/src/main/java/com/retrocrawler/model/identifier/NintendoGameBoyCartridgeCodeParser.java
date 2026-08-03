package com.retrocrawler.model.identifier;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.retrocrawler.core.catalog.CatalogLoader;
import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.AbstractCatalogFactParser;

public final class NintendoGameBoyCartridgeCodeParser
		extends AbstractCatalogFactParser<NintendoGameBoyCartridgeCatalogKey, NintendoGameBoyCartridgeCode> {

	private final NintendoGameBoyCartridgeCatalog cartridgeCatalog;

	public NintendoGameBoyCartridgeCodeParser() {
		this(NintendoGameBoyCartridgeCatalog.bundled());
	}

	public NintendoGameBoyCartridgeCodeParser(final CatalogLoader catalogs) {
		super(catalogs, NintendoGameBoyCartridgeCatalogKey.class, NintendoGameBoyCartridgeCatalog.DEFAULT_CATALOG_FILE);
		cartridgeCatalog = NintendoGameBoyCartridgeCatalog.from(catalog());
	}

	public NintendoGameBoyCartridgeCodeParser(final NintendoGameBoyCartridgeCatalog catalog) {
		super(Objects.requireNonNull(catalog, "catalog").catalog());
		cartridgeCatalog = catalog;
	}

	@Override
	public RatedFact<NintendoGameBoyCartridgeCode> parse(final String rawValue) {
		if (rawValue == null) {
			return noMatch();
		}

		try {
			final NintendoGameBoyCartridgeCode code = new NintendoGameBoyCartridgeCode(
					rawValue.trim().toUpperCase(Locale.ROOT));
			if (!hasPlausiblePlatformMarker(code.segments())) {
				return noMatch();
			}
			return cartridgeCatalog.contains(code) ? RatedFact.exact(code) : RatedFact.strong(code);
		} catch (final IllegalArgumentException e) {
			return noMatch();
		}
	}

	private static boolean hasPlausiblePlatformMarker(final List<String> segments) {
		for (int index = 0; index < Math.min(2, segments.size()); index++) {
			if (NintendoGameBoyPlatform.fromCode(segments.get(index)).isPresent()) {
				return true;
			}
		}

		final String first = segments.getFirst();
		if (first.length() != 3) {
			return false;
		}
		for (final NintendoGameBoyPlatform platform : NintendoGameBoyPlatform.values()) {
			if (differsByAtMostOneCharacter(first, platform.code())) {
				return true;
			}
		}
		return false;
	}

	private static boolean differsByAtMostOneCharacter(final String left, final String right) {
		int differences = 0;
		for (int index = 0; index < left.length(); index++) {
			if (left.charAt(index) != right.charAt(index) && ++differences > 1) {
				return false;
			}
		}
		return true;
	}

	private static RatedFact<NintendoGameBoyCartridgeCode> noMatch() {
		return RatedFact.none("Expected a plausible Nintendo Game Boy-family cartridge label code.");
	}
}
