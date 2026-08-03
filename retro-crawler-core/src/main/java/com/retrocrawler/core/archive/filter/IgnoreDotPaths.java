package com.retrocrawler.core.archive.filter;

import java.nio.file.Path;

/**
 * Rejects entries whose file name begins with a dot.
 */
public final class IgnoreDotPaths implements ArchivePathFilter {

	@Override
	public boolean accept(final Path path) {
		final String name = ArchivePathNames.name(path);
		return name == null || !name.startsWith(".");
	}
}
