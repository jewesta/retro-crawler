package com.retrocrawler.core.archive;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class IgnoreSystemFilesTest {

	private final CrawlPolicy policy = new IgnoreSystemFiles();

	@Test
	void ignoresDotPrefixedEntriesWithoutFilesystemMetadataChecks() {
		for (final String name : List.of(".DS_Store", "._photo.jpg", ".@__thumb", ".git", ".metadata")) {
			assertFalse(policy.includes(Path.of("archive", name)), name);
		}
	}

	@Test
	void ignoresCommonVisibleSystemAndNasEntriesCaseInsensitively() {
		for (final String name : List.of("Thumbs.db", "DESKTOP.INI", "__MACOSX", "$RECYCLE.BIN",
				"System Volume Information", "@Recycle", "@eaDir", "#recycle", "lost+found")) {
			assertFalse(policy.includes(Path.of("archive", name)), name);
		}
	}

	@Test
	void admitsOrdinaryArchiveEntries() {
		for (final String name : List.of("photos", "retro.md", "Manufacturer Logos", "Icon.png")) {
			assertTrue(policy.includes(Path.of("archive", name)), name);
		}
	}
}
