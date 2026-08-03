package com.retrocrawler.core.archive.filter;

import java.nio.file.Path;
import java.util.Set;

/**
 * Rejects common QNAP service entries.
 */
public final class IgnoreQNAPSystemPaths implements ArchivePathFilter {

	private static final Set<String> NAMES = Set.of(
			".@__thumb",
			"@recycle");

	@Override
	public boolean accept(final Path path) {
		return !ArchivePathNames.matches(path, NAMES);
	}
}
