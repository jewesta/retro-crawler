package com.retrocrawler.app;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.retrocrawler.core.Model;
import com.retrocrawler.core.RetroCrawler;
import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveRoots;
import com.retrocrawler.core.archive.Repository;
import com.retrocrawler.core.archive.source.ArchiveSource;
import com.retrocrawler.demo.DemoModels;

record DemoArchive(String label, RetroCrawler crawler, Optional<LocalArchiveFolderOpener> folderOpener) {

	DemoArchive {
		Objects.requireNonNull(label, "label");
		Objects.requireNonNull(crawler, "crawler");
		Objects.requireNonNull(folderOpener, "folderOpener");
	}

	static DemoArchive create(final DemoModels demoModel, final DemoArchiveSource sourceOption,
			final Repository repository) throws IOException {
		Objects.requireNonNull(demoModel, "demoModel");
		Objects.requireNonNull(sourceOption, "sourceOption");
		Objects.requireNonNull(repository, "repository");

		final Model declaredModel = Model.from(demoModel.getBasePackage());
		final ArchiveDescriptor declaredArchive = declaredModel.archiveDescriptor();
		final List<Path> roots = sourceOption.roots(declaredArchive);
		sourceOption.materialize(declaredArchive, roots);

		final Model model = Model.from(demoModel.getBasePackage(), ArchiveRoots.from(roots));
		final ArchiveSource source = sourceOption.createSource();
		final RetroCrawler crawler = RetroCrawler.builder().model(model).repository(repository).archiveSource(source)
				.build();
		final Optional<LocalArchiveFolderOpener> folderOpener = sourceOption.localFolders()
				? Optional.of(new LocalArchiveFolderOpener())
				: Optional.empty();
		return new DemoArchive(crawler.archiveDescriptor().name() + " — " + sourceOption.label(), crawler,
				folderOpener);
	}
}
