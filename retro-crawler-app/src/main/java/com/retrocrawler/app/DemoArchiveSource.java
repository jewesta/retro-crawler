package com.retrocrawler.app;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
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
		List<Path> roots(final ArchiveDescriptor descriptor) {
			return List.copyOf(descriptor.paths());
		}

		@Override
		void materialize(final ArchiveDescriptor descriptor, final List<Path> roots) throws IOException {
			DemoFiles.copyToWorkDirectory(descriptor);
		}
	},

	ZIP("ZIP archive", "_zip", false) {

		@Override
		ArchiveSource createSource() {
			return new ZipArchiveSource();
		}

		@Override
		List<Path> roots(final ArchiveDescriptor descriptor) {
			return descriptor.paths().stream().map(DemoArchiveSource::zipPath).toList();
		}

		@Override
		void materialize(final ArchiveDescriptor descriptor, final List<Path> roots) throws IOException {
			for (final Path root : roots) {
				DemoFiles.copyFileToWorkDirectory(root);
			}
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

	ArchiveDescriptor archive(final ArchiveDescriptor declaredArchive, final List<Path> roots) {
		Objects.requireNonNull(declaredArchive, "declaredArchive");
		Objects.requireNonNull(roots, "roots");
		final ArchiveId archiveId = archiveIdSuffix.isEmpty() ? declaredArchive.id()
				: ArchiveId.of(declaredArchive.id().value() + archiveIdSuffix);
		return new ArchiveDescriptor(archiveId, declaredArchive.name(), roots);
	}

	abstract ArchiveSource createSource();

	abstract List<Path> roots(ArchiveDescriptor descriptor);

	abstract void materialize(ArchiveDescriptor descriptor, List<Path> roots) throws IOException;

	private static Path zipPath(final Path folder) {
		final Path fileName = Objects.requireNonNull(folder.getFileName(), "archive root file name");
		return folder.resolveSibling(fileName + ".zip");
	}
}
