package com.retrocrawler.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.archive.CrawlPlanning;
import com.retrocrawler.core.archive.JsonFileRepository;
import com.retrocrawler.core.archive.Repository;
import com.retrocrawler.core.archive.source.ArchiveSource;
import com.retrocrawler.core.archive.source.FileSystemArchiveSource;

final class DefaultRetroCrawlerBuilder implements RetroCrawler.Builder {

	private Model model;

	private Repository repository;

	private CrawlPlanning crawlPlanning;

	private final Map<ArchiveId, ArchiveBinding> archives = new LinkedHashMap<>();

	private boolean locationsConfigured;

	@Override
	public RetroCrawler.Builder model(final Model model) {
		if (this.model != null) {
			throw new IllegalStateException("Model is already configured.");
		}
		this.model = Objects.requireNonNull(model, "model");
		return this;
	}

	@Override
	public RetroCrawler.Builder locations(final Locations locations) {
		Objects.requireNonNull(locations, "locations");
		if (repository != null || !archives.isEmpty()) {
			throw new IllegalStateException(
					"Locations cannot be combined with explicit repository or archive configuration.");
		}

		locationsConfigured = true;
		repository = new JsonFileRepository(locations.repositoryRoot());
		for (final ArchiveDescriptor archive : locations.archives()) {
			archives.put(archive.id(), new ArchiveBinding(archive, new FileSystemArchiveSource()));
		}
		return this;
	}

	@Override
	public RetroCrawler.Builder repository(final Repository repository) {
		if (locationsConfigured) {
			throw new IllegalStateException(
					"Explicit repository configuration cannot be combined with configured locations.");
		}
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
	public RetroCrawler.Builder archive(final ArchiveDescriptor archive) {
		return archive(archive, new FileSystemArchiveSource());
	}

	@Override
	public RetroCrawler.Builder archive(final ArchiveDescriptor archive, final ArchiveSource source) {
		if (locationsConfigured) {
			throw new IllegalStateException(
					"Explicit archive configuration cannot be combined with configured locations.");
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
		if (archives.isEmpty()) {
			throw new IllegalStateException("At least one archive must be configured.");
		}

		final CrawlPlanning effectivePlanning = crawlPlanning == null ? CrawlPlanning.defaults() : crawlPlanning;
		return new RetroCrawlerImpl(model, List.copyOf(archives.values()), effectivePlanning, repository);
	}
}
