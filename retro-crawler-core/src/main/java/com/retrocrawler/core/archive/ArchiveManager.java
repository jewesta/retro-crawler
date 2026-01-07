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

public class ArchiveManager {

	private static final Logger logger = LoggerFactory.getLogger(ArchiveManager.class);

	public static final Path CACHE_DIRECTORY = Path.of("cache");

	private final ObjectMapper mapper = new ObjectMapper();

	private Archive cache;

	private final Path cacheDirectory;

	private final ArchiveDescriptor descriptor;

	private final ArchiveDigger digger;

	public ArchiveManager(final ArchiveDescriptor descriptor, final ArchiveDigger digger) {
		this.cacheDirectory = CACHE_DIRECTORY;
		this.descriptor = Objects.requireNonNull(descriptor);
		this.digger = Objects.requireNonNull(digger);
	}

	private Path getJsonPath() throws IOException {
		Files.createDirectories(cacheDirectory);
		final Path tmpFile = cacheDirectory.resolve("archive_" + descriptor.getId() + ".json");
		return tmpFile;
	}

	private Optional<Archive> fromCache() {
		try {
			final Path jsonPath = getJsonPath();
			if (Files.exists(jsonPath)) {
				final File jsonFile = jsonPath.toFile();
				logger.info("Loading archive from cache at: " + jsonFile + ".");
				final Archive stash = mapper.readValue(jsonFile, Archive.class);
				logger.info("Archive loaded.");
				return Optional.ofNullable(stash);
			}
			return Optional.empty();
		} catch (final IOException e) {
			logger.error("Failed to load cache: " + e.getMessage() + " Cache will be rebuilt.", e);
			return Optional.empty();
		}
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
		final File jsonFile = getJsonPath().toFile();
		mapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile, archive);
		logger.info("Cache at: " + jsonFile.toString());
		return archive;
	}

	public synchronized Archive getArchive(final Monitor monitor, final boolean refreshCache) throws IOException {
		if (!refreshCache) {
			if (cache != null) {
				return cache;
			}
			cache = fromCache().orElse(null);
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
