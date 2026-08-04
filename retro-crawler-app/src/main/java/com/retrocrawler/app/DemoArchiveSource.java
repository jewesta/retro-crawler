package com.retrocrawler.app;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.archive.source.ArchiveSource;
import com.retrocrawler.core.archive.source.FileSystemArchiveSource;
import com.retrocrawler.core.archive.source.ZipArchiveSource;
import com.retrocrawler.demo.DemoFiles;

enum DemoArchiveSource {

	FILE_SYSTEM("File system", "", true) {

		@Override
		ArchiveSource createSource() {
			return new FileSystemArchiveSource();
		}

		@Override
		Path root(final Path declaredRoot) {
			return declaredRoot;
		}

		@Override
		void materialize(final ArchiveDescriptor archive) throws IOException {
			DemoFiles.copyToWorkDirectory(archive);
		}
	},

	ZIP("ZIP archive", "_zip", false) {

		@Override
		ArchiveSource createSource() {
			return new ZipArchiveSource();
		}

		@Override
		Path root(final Path declaredRoot) {
			return zipPath(declaredRoot);
		}

		@Override
		void materialize(final ArchiveDescriptor archive) throws IOException {
			DemoFiles.copyFileToWorkDirectory(archive.root());
		}
	};

	private final String label;

	private final String archiveIdSuffix;

	private final boolean localFolders;

	DemoArchiveSource(final String label, final String archiveIdSuffix, final boolean localFolders) {
		this.label = label;
		this.archiveIdSuffix = archiveIdSuffix;
		this.localFolders = localFolders;
	}

	String label() {
		return label;
	}

	boolean localFolders() {
		return localFolders;
	}

	/**
	 * Derives the archive this provider exposes from the demo's declared
	 * archive.
	 */
	ArchiveDescriptor archive(final ArchiveDescriptor declaredArchive) {
		Objects.requireNonNull(declaredArchive, "declaredArchive");
		final ArchiveId archiveId = archiveIdSuffix.isEmpty() ? declaredArchive.id()
				: ArchiveId.of(declaredArchive.id().value() + archiveIdSuffix);
		return new ArchiveDescriptor(archiveId, declaredArchive.name(), root(declaredArchive.root()));
	}

	abstract ArchiveSource createSource();

	abstract Path root(Path declaredRoot);

	abstract void materialize(ArchiveDescriptor archive) throws IOException;

	private static Path zipPath(final Path folder) {
		final Path fileName = Objects.requireNonNull(folder.getFileName(), "archive root file name");
		return folder.resolveSibling(fileName + ".zip");
	}
}
