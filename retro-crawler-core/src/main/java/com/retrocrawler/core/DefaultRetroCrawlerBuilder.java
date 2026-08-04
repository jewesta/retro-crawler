package com.retrocrawler.core;

import java.util.Objects;

import com.retrocrawler.core.archive.ArchiveDigger;
import com.retrocrawler.core.archive.CrawlPlanning;
import com.retrocrawler.core.archive.Repository;
import com.retrocrawler.core.archive.source.ArchiveSource;
import com.retrocrawler.core.archive.source.FileSystemArchiveSource;

final class DefaultRetroCrawlerBuilder implements RetroCrawler.Builder {

	private Model model;

	private Repository repository;

	private CrawlPlanning crawlPlanning;

	private ArchiveSource archiveSource;

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
	public RetroCrawler.Builder crawlPlanning(final CrawlPlanning planning) {
		if (this.crawlPlanning != null) {
			throw new IllegalStateException("Crawl planning is already configured.");
		}
		this.crawlPlanning = Objects.requireNonNull(planning, "planning");
		return this;
	}

	@Override
	public RetroCrawler.Builder archiveSource(final ArchiveSource source) {
		if (archiveSource != null) {
			throw new IllegalStateException("Archive source is already configured.");
		}
		archiveSource = Objects.requireNonNull(source, "source");
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

		final CrawlPlanning effectivePlanning = crawlPlanning == null ? CrawlPlanning.defaults() : crawlPlanning;
		final ArchiveSource effectiveSource = archiveSource == null ? new FileSystemArchiveSource() : archiveSource;
		final ArchiveDigger digger = new ArchiveDigger(model, effectiveSource, effectivePlanning);
		return new RetroCrawlerImpl(model.archiveDescriptor(), digger, model.gearResolver(), model.configuration(),
				repository);
	}
}
