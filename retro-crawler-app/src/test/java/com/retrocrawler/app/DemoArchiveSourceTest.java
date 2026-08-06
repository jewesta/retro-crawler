package com.retrocrawler.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.archive.source.FileSystemArchiveSource;
import com.retrocrawler.core.archive.source.ZipArchiveSource;

class DemoArchiveSourceTest {

	private static final Path FOLDER = Path.of("rc_demo_archives/retro_pc");

	private static final ArchiveDescriptor ARCHIVE = new ArchiveDescriptor(ArchiveId.of("demo"), "Demo", FOLDER);

	@Test
	void keepsTheDeclaredRootForTheFilesystemProvider() {
		final ArchiveDescriptor archive = DemoArchiveSource.FILE_SYSTEM.archive(ARCHIVE);
		assertEquals(FOLDER, archive.root());
		assertEquals(ArchiveId.of("demo"), archive.id());
		assertInstanceOf(FileSystemArchiveSource.class, DemoArchiveSource.FILE_SYSTEM.createSource());
		assertTrue(DemoArchiveSource.FILE_SYSTEM.localFolders());
	}

	@Test
	void mapsTheDeclaredFolderToAnAdjacentZipFileUnderItsOwnArchiveId() {
		final ArchiveDescriptor archive = DemoArchiveSource.ZIP.archive(ARCHIVE);
		assertEquals(Path.of("rc_demo_archives/retro_pc.zip"), archive.root());
		assertEquals(ArchiveId.of("demo_zip"), archive.id());
		assertInstanceOf(ZipArchiveSource.class, DemoArchiveSource.ZIP.createSource());
		assertFalse(DemoArchiveSource.ZIP.localFolders());
	}
}
