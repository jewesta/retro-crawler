package com.retrocrawler.core.archive;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retrocrawler.core.archive.clues.Archive;
import com.retrocrawler.core.archive.clues.ArchiveNode;
import com.retrocrawler.core.archive.clues.Bucket;
import com.retrocrawler.core.util.Monitor;
import com.retrocrawler.core.util.ReadmeWriter;

public class ArchiveManager {

	private static final Logger logger = LoggerFactory.getLogger(ArchiveManager.class);

	private Archive cache;

	private final ArchiveDescriptor descriptor;

	private final Repository repository = new JsonFileRepository();

	private final ArchiveDigger digger;

	public ArchiveManager(final ArchiveDescriptor descriptor, final ArchiveDigger digger) {
		this.descriptor = Objects.requireNonNull(descriptor);
		this.digger = Objects.requireNonNull(digger);
	}

	private Archive fromFileSystem(final Monitor monitor) throws IOException {
		final Collection<Path> rootPaths = descriptor.getPaths();
		final List<Bucket> buckets = new ArrayList<>();
		for (final Path rootPath : rootPaths) {
			final ArchiveNode rootNode = digger.dig(rootPath, monitor);
			final Bucket bucket = Bucket.of(rootPath, rootNode);
			buckets.add(bucket);
		}
		final Archive archive = Archive.of(descriptor.getId(), buckets);
		repository.stowaway(archive);
		return archive;
	}

	public synchronized Archive getArchive(final Monitor monitor, final boolean refreshCache) throws IOException {
		if (!refreshCache) {
			if (cache != null) {
				return cache;
			}
			cache = repository.retrieve(descriptor.getId()).orElse(null);
			if (cache != null) {
				return cache;
			}
		}
		cache = fromFileSystem(monitor);
		return cache;
	}

	public Archive getArchive(final Monitor monitor) throws IOException {
		return getArchive(monitor, false);
	}

}
