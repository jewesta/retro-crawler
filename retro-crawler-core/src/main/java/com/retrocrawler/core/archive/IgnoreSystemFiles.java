package com.retrocrawler.core.archive;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

/**
 * Ignores dot-prefixed entries and common operating-system or NAS service
 * entries. Name checks deliberately avoid filesystem metadata calls so the
 * policy remains cheap on network archives.
 */
public final class IgnoreSystemFiles implements CrawlPolicy {

	private static final Set<String> SYSTEM_ENTRY_NAMES = Set.of(
			"#recycle",
			"$recycle.bin",
			"@eadir",
			"@recycle",
			"__macosx",
			"desktop.ini",
			"ehthumbs.db",
			"icon\r",
			"lost+found",
			"system volume information",
			"thumbs.db");

	@Override
	public boolean includes(final Path entry) {
		final Path fileName = entry.getFileName();
		if (fileName == null) {
			return true;
		}
		final String name = fileName.toString();
		return !name.startsWith(".") && !SYSTEM_ENTRY_NAMES.contains(name.toLowerCase(Locale.ROOT));
	}
}
