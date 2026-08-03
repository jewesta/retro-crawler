package com.retrocrawler.core.archive.filter;

import java.nio.file.Path;
import java.util.Set;

/**
 * Rejects common macOS service entries and AppleDouble sidecar files.
 */
public final class IgnoreMacSystemPaths implements ArchivePathFilter {

	private static final Set<String> NAMES = Set.of(".ds_store", "__macosx", "icon\r");

	@Override
	public boolean accept(final Path path) {
		final String name = ArchivePathNames.name(path);
		return (name == null || !name.startsWith("._")) && !ArchivePathNames.matches(path, NAMES);
	}
}
