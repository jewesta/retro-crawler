package com.retrocrawler.core.archive;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.filter.ArchivePathFilter;
import com.retrocrawler.core.archive.filter.IgnoreDotPaths;
import com.retrocrawler.core.archive.filter.IgnoreLinuxSystemPaths;
import com.retrocrawler.core.archive.filter.IgnoreMacSystemPaths;
import com.retrocrawler.core.archive.filter.IgnoreQNAPSystemPaths;
import com.retrocrawler.core.archive.filter.IgnoreSynologySystemPaths;
import com.retrocrawler.core.archive.filter.IgnoreWindowsSystemPaths;

class SystemPathFiltersTest {

	@Test
	void ignoresDotPrefixedEntriesWithoutFilesystemMetadataChecks() {
		assertRejected(new IgnoreDotPaths(), ".DS_Store", "._photo.jpg", ".@__thumb", ".git", ".metadata");
	}

	@Test
	void ignoresWindowsSystemPathsCaseInsensitively() {
		assertRejected(new IgnoreWindowsSystemPaths(), "Thumbs.db", "DESKTOP.INI", "$RECYCLE.BIN",
				"System Volume Information", "ehthumbs.db");
	}

	@Test
	void ignoresMacSystemPathsCaseInsensitively() {
		assertRejected(new IgnoreMacSystemPaths(), ".DS_Store", "._photo.jpg", "__MACOSX", "Icon\r");
	}

	@Test
	void ignoresLinuxSystemPathsCaseInsensitively() {
		assertRejected(new IgnoreLinuxSystemPaths(), "lost+found", "LOST+FOUND");
	}

	@Test
	void ignoresQnapSystemPathsCaseInsensitively() {
		assertRejected(new IgnoreQNAPSystemPaths(), ".@__thumb", "@Recycle");
	}

	@Test
	void ignoresSynologySystemPathsCaseInsensitively() {
		assertRejected(new IgnoreSynologySystemPaths(), "@eaDir", "#recycle");
	}

	@Test
	void admitsOrdinaryArchiveEntries() {
		final List<ArchivePathFilter> filters = List.of(new IgnoreDotPaths(), new IgnoreWindowsSystemPaths(),
				new IgnoreMacSystemPaths(), new IgnoreLinuxSystemPaths(), new IgnoreQNAPSystemPaths(),
				new IgnoreSynologySystemPaths());

		for (final ArchivePathFilter filter : filters) {
			for (final String name : List.of("photos", "retro.md", "Manufacturer Logos", "Icon.png")) {
				assertTrue(filter.accept(Path.of("archive", name)), filter.getClass().getSimpleName() + ": " + name);
			}
		}
	}

	private static void assertRejected(final ArchivePathFilter filter, final String... names) {
		for (final String name : names) {
			assertFalse(filter.accept(Path.of("archive", name)), name);
		}
	}
}
