package com.retrocrawler.demo;

import java.nio.file.Path;

import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveId;

/**
 * The demo models and the archive each one ships its sample material in.
 * <p>
 * Archive roots are deployment configuration rather than model vocabulary, so
 * the demo declares them here instead of on {@code @RetroCollection}.
 */
public enum DemoModels {

	RETRO_PC("com.retrocrawler.demo.gear", "retro_pc_demo", "Retro PC (Demo)",
			DemoFiles.DEMO_ARCHIVE_PARENT + "/retro_pc");

	private final String basePackage;

	private final ArchiveDescriptor archive;

	private DemoModels(final String basePackage, final String archiveId, final String archiveName,
			final String archiveRoot) {
		this.basePackage = basePackage;
		this.archive = new ArchiveDescriptor(ArchiveId.of(archiveId), archiveName, Path.of(archiveRoot));
	}

	public String getBasePackage() {
		return basePackage;
	}

	/**
	 * The archive this demo model reads, rooted below the demo work directory.
	 */
	public ArchiveDescriptor getArchive() {
		return archive;
	}

}
