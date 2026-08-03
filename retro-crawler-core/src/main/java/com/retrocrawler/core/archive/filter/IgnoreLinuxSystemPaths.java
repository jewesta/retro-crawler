package com.retrocrawler.core.archive.filter;

import java.nio.file.Path;
import java.util.Set;

/**
 * Rejects common Linux filesystem service entries.
 */
public final class IgnoreLinuxSystemPaths implements ArchivePathFilter {

	private static final Set<String> NAMES = Set.of("lost+found");

	@Override
	public boolean accept(final Path path) {
		return !ArchivePathNames.matches(path, NAMES);
	}
}
