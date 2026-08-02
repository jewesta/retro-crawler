package com.retrocrawler.mycollection.facts;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.model.storage.HardDiskDriveFormFactor;
import com.retrocrawler.model.storage.HardDiskDriveFormFactorParser;

/**
 * Restricts anonymous hard-drive form factors to the unambiguous size currently
 * established in this collection. In particular, 3.5 and 5.25-inch tags remain
 * available to the floppy-disk form-factor parser.
 */
public final class CollectionHardDiskDriveFormFactorParser implements FactParser {

	private final HardDiskDriveFormFactorParser delegate = new HardDiskDriveFormFactorParser();

	@Override
	public RatedFact parse(final String rawValue) {
		final RatedFact parsed = delegate.parse(rawValue);
		if (parsed.getValue().filter(HardDiskDriveFormFactor.INCH_2_5::equals).isPresent()) {
			return parsed;
		}
		return RatedFact.none("Expected the collection's established 2.5-inch hard-drive form factor.");
	}
}
