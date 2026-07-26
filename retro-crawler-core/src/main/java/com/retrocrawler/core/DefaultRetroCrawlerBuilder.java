package com.retrocrawler.core;

import java.util.Objects;

import com.retrocrawler.core.archive.ArchiveDigger;
import com.retrocrawler.core.archive.Repository;

final class DefaultRetroCrawlerBuilder implements RetroCrawler.Builder {

	private Model model;

	private Repository repository;

	@Override
	public RetroCrawler.Builder model(final Model model) {
		if (this.model != null) {
			throw new IllegalStateException("Model is already configured.");
		}
		this.model = Objects.requireNonNull(model, "model");
		return this;
	}

	@Override
	public RetroCrawler.Builder repository(final Repository repository) {
		if (this.repository != null) {
			throw new IllegalStateException("Repository is already configured.");
		}
		this.repository = Objects.requireNonNull(repository, "repository");
		return this;
	}

	@Override
	public RetroCrawler build() {
		if (model == null) {
			throw new IllegalStateException("Missing required model configuration.");
		}
		if (repository == null) {
			throw new IllegalStateException("Missing required repository configuration.");
		}

		final ArchiveDigger digger = new ArchiveDigger(model.getArchiveDescriptor(), model.archivePathClueFinder());
		return new RetroCrawlerImpl(model.getArchiveDescriptor(), digger, model.gearResolver(), repository);
	}
}
