package com.retrocrawler.core.archive;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.retrocrawler.core.archive.clues.Archive;
import com.retrocrawler.core.archive.clues.ArchiveNode;
import com.retrocrawler.core.archive.clues.Bucket;
import com.retrocrawler.core.progress.ProgressStage;
import com.retrocrawler.core.progress.Progressor;

public class ArchiveManager {

	private static final Logger logger = LoggerFactory.getLogger(ArchiveManager.class);

	private Archive cache;

	private final ArchiveDescriptor descriptor;

	private final Repository repository;

	private final ArchiveDigger digger;

	public ArchiveManager(final ArchiveDescriptor descriptor, final ArchiveDigger digger,
			final Repository repository) {
		this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
		this.digger = Objects.requireNonNull(digger, "digger");
		this.repository = Objects.requireNonNull(repository, "repository");
	}

	private Archive fromFileSystem(final Progressor progressor) throws IOException {
		final Collection<Path> rootPaths = descriptor.getPaths();
		final ArchiveDigPlan plan = digger.plan(rootPaths, progressor);
		final List<Bucket> buckets = new ArrayList<>();
		for (final Path rootPath : rootPaths) {
			progressor.throwIfCancelled();
			final ArchiveNode rootNode = digger.dig(rootPath, plan, progressor);
			final Bucket bucket = Bucket.of(rootPath, rootNode);
			buckets.add(bucket);
		}
		final Archive archive = Archive.of(descriptor.getId(), buckets);
		progressor.throwIfCancelled();
		progressor.indeterminate(ProgressStage.STOWING, "Stowing away the extracted clue archive.");
		repository.stowaway(archive);
		return archive;
	}

	public synchronized Archive getArchive(final Progressor progressor, final boolean refreshCache) throws IOException {
		if (!refreshCache) {
			if (cache != null) {
				return cache;
			}
			cache = retrieve().orElse(null);
			if (cache != null) {
				return cache;
			}
		}
		cache = fromFileSystem(progressor);
		return cache;
	}

	public Archive getArchive(final Progressor progressor) throws IOException {
		return getArchive(progressor, false);
	}

	private Optional<Archive> retrieve() {
		try {
			return repository.retrieve(descriptor.getId());
		} catch (final RepositoryException e) {
			logger.warn("Could not retrieve archive '{}'. The filesystem archive will be crawled again.",
					descriptor.getId(), e);
			return Optional.empty();
		}
	}

}
