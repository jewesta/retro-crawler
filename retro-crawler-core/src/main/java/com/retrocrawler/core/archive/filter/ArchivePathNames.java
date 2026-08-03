package com.retrocrawler.core.archive.filter;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

final class ArchivePathNames {

	private ArchivePathNames() {
	}

	static String name(final Path path) {
		final Path fileName = path.getFileName();
		return fileName == null ? null : fileName.toString();
	}

	static boolean matches(final Path path, final Set<String> lowercaseNames) {
		final String name = name(path);
		return name != null && lowercaseNames.contains(name.toLowerCase(Locale.ROOT));
	}
}
