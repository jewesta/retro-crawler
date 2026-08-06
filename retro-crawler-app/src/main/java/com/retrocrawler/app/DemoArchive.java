package com.retrocrawler.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.retrocrawler.core.Model;
import com.retrocrawler.core.RetroCrawler;
import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.Repository;
import com.retrocrawler.demo.DemoModels;

record DemoArchive(String label, RetroCrawler crawler, ArchiveDescriptor archive,
		Optional<LocalArchiveFolderOpener> folderOpener) {

	DemoArchive {
		Objects.requireNonNull(label, "label");
		Objects.requireNonNull(crawler, "crawler");
		Objects.requireNonNull(archive, "archive");
		Objects.requireNonNull(folderOpener, "folderOpener");
	}

	static List<DemoArchive> create(final DemoModels demoModel, final Repository repository) throws IOException {
		Objects.requireNonNull(demoModel, "demoModel");
		Objects.requireNonNull(repository, "repository");

		final Model model = Model.from(demoModel.getBasePackage());
		final ArchiveDescriptor declaredArchive = demoModel.getArchive();
		final RetroCrawler.Builder builder = RetroCrawler.builder().model(model).repository(repository);
		final List<ConfiguredArchive> configuredArchives = new ArrayList<>();
		for (final DemoArchiveSource sourceOption : DemoArchiveSource.values()) {
			final ArchiveDescriptor archive = sourceOption.archive(declaredArchive);
			sourceOption.materialize(archive);
			builder.archive(archive, sourceOption.createSource());
			configuredArchives.add(new ConfiguredArchive(sourceOption, archive));
		}

		final RetroCrawler crawler = builder.build();
		return configuredArchives.stream().map(configured -> configured.toDemoArchive(crawler)).toList();
	}

	private record ConfiguredArchive(DemoArchiveSource source, ArchiveDescriptor archive) {

		DemoArchive toDemoArchive(final RetroCrawler crawler) {
			final Optional<LocalArchiveFolderOpener> folderOpener = source.localFolders()
					? Optional.of(new LocalArchiveFolderOpener())
					: Optional.empty();
			return new DemoArchive(archive.name() + " — " + source.label(), crawler, archive, folderOpener);
		}
	}
}
