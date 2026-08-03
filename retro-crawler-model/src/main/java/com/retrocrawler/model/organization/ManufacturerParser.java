package com.retrocrawler.model.organization;

import java.util.List;
import java.util.Objects;

import com.retrocrawler.core.catalog.CatalogLoader;
import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.AbstractCatalogFactParser;

/**
 * Resolves exact short and full manufacturer names from a selected catalog.
 */
public final class ManufacturerParser extends AbstractCatalogFactParser<ManufacturerCatalogKey> {

	private final ManufacturerCatalog manufacturerCatalog;

	public ManufacturerParser() {
		this(ManufacturerCatalog.bundled());
	}

	public ManufacturerParser(final CatalogLoader catalogs) {
		super(catalogs, ManufacturerCatalogKey.class, ManufacturerCatalog.DEFAULT_CATALOG_FILE);
		manufacturerCatalog = ManufacturerCatalog.from(catalog());
	}

	public ManufacturerParser(final ManufacturerCatalog catalog) {
		super(Objects.requireNonNull(catalog, "catalog").catalog());
		manufacturerCatalog = catalog;
	}

	@Override
	public RatedFact parse(final String rawValue) {
		if (rawValue == null || rawValue.isBlank()) {
			return noMatch("Expected a manufacturer name present in the selected catalog.");
		}

		final List<Manufacturer> matches = manufacturerCatalog.findByName(rawValue);
		if (matches.size() == 1) {
			return RatedFact.exact(matches.getFirst());
		}
		if (matches.size() > 1) {
			return noMatch("Manufacturer name is ambiguous in the selected catalog.");
		}
		return noMatch("Manufacturer name is not present in the selected catalog.");
	}

	private static RatedFact noMatch(final String explanation) {
		return RatedFact.none(explanation);
	}
}
