package com.retrocrawler.core.archive.filter;

import java.nio.file.Path;
import java.util.Set;

/**
 * Rejects common Microsoft Windows service entries.
 */
public final class IgnoreWindowsSystemPaths implements ArchivePathFilter {

	private static final Set<String> NAMES = Set.of(
			"$recycle.bin",
			"desktop.ini",
			"ehthumbs.db",
			"system volume information",
			"thumbs.db");

	@Override
	public boolean accept(final Path path) {
		return !ArchivePathNames.matches(path, NAMES);
	}
}
