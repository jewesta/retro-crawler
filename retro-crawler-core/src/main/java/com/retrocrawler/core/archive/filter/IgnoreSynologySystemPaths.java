package com.retrocrawler.core.archive.filter;

import java.nio.file.Path;
import java.util.Set;

/**
 * Rejects common Synology service entries.
 */
public final class IgnoreSynologySystemPaths implements ArchivePathFilter {

	private static final Set<String> NAMES = Set.of(
			"#recycle",
			"@eadir");

	@Override
	public boolean accept(final Path path) {
		return !ArchivePathNames.matches(path, NAMES);
	}
}
