package com.retrocrawler.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.archive.CrawlPlanning;
import com.retrocrawler.core.archive.Repository;
import com.retrocrawler.core.archive.source.ArchiveSource;
import com.retrocrawler.core.archive.source.FileSystemArchiveSource;

final class DefaultRetroCrawlerBuilder implements RetroCrawler.Builder {

	private Model model;

	private Repository repository;

	private CrawlPlanning crawlPlanning;

	private ArchiveSource archiveSource;

	private final Map<ArchiveId, ArchiveBinding> archives = new LinkedHashMap<>();

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
		if (!archives.isEmpty()) {
			throw new IllegalStateException("A default archive source cannot be combined with explicit archives.");
		}
		archiveSource = Objects.requireNonNull(source, "source");
		return this;
	}

	@Override
	public RetroCrawler.Builder archive(final ArchiveDescriptor archive) {
		return archive(archive, new FileSystemArchiveSource());
	}

	@Override
	public RetroCrawler.Builder archive(final ArchiveDescriptor archive, final ArchiveSource source) {
		if (archiveSource != null) {
			throw new IllegalStateException("Explicit archives cannot be combined with a default archive source.");
		}
		final ArchiveBinding binding = new ArchiveBinding(archive, source);
		if (archives.putIfAbsent(binding.descriptor().id(), binding) != null) {
			throw new IllegalArgumentException("Archive is already configured: " + binding.descriptor().id());
		}
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
		final List<ArchiveBinding> effectiveArchives;
		if (archives.isEmpty()) {
			final ArchiveSource effectiveSource = archiveSource == null ? new FileSystemArchiveSource() : archiveSource;
			effectiveArchives = List.of(new ArchiveBinding(model.archiveDescriptor(), effectiveSource));
		} else {
			effectiveArchives = List.copyOf(archives.values());
		}
		return new RetroCrawlerImpl(model, effectiveArchives, effectivePlanning, repository);
	}
}
