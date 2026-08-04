package com.retrocrawler.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.archive.source.FileSystemArchiveSource;
import com.retrocrawler.core.archive.source.ZipArchiveSource;

class DemoArchiveSourceTest {

	private static final Path FOLDER = Path.of("rc_demo_archives/retro_pc");

	private static final ArchiveDescriptor ARCHIVE = new ArchiveDescriptor(ArchiveId.of("demo"), "Demo",
			List.of(FOLDER));

	@Test
	void keepsDeclaredRootsForTheFilesystemProvider() {
		assertEquals(List.of(FOLDER), DemoArchiveSource.FILE_SYSTEM.roots(ARCHIVE));
		assertInstanceOf(FileSystemArchiveSource.class, DemoArchiveSource.FILE_SYSTEM.createSource());
		assertTrue(DemoArchiveSource.FILE_SYSTEM.localFolders());
	}

	@Test
	void mapsDeclaredFoldersToAdjacentZipFiles() {
		assertEquals(List.of(Path.of("rc_demo_archives/retro_pc.zip")), DemoArchiveSource.ZIP.roots(ARCHIVE));
		assertInstanceOf(ZipArchiveSource.class, DemoArchiveSource.ZIP.createSource());
		assertFalse(DemoArchiveSource.ZIP.localFolders());
	}
}
