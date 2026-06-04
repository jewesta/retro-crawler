package com.retrocrawler.core.archive;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retrocrawler.core.archive.clues.Archive;
import com.retrocrawler.core.util.ReadmeWriter;

public class JsonFileRepository implements Repository {

	public static final Path CACHE_DIRECTORY = Path.of("cache");

	// As per javadoc recommemded and thread safe
	private final ObjectMapper mapper = new ObjectMapper();

	private static final Logger logger = LoggerFactory.getLogger(JsonFileRepository.class);

	private Path getJsonPath(final ArchiveId id) {
		if (!Files.exists(CACHE_DIRECTORY)) {
			// Does NOT throw if the directory already exists.
			try {
				Files.createDirectories(CACHE_DIRECTORY);
			} catch (final IOException e) {
				throw new RepositoryException(
						"Could not create directory for json file at: " + CACHE_DIRECTORY.toString(), e);
			}
			try {
				ReadmeWriter.writeReadmeTemporary(CACHE_DIRECTORY);
			} catch (final IOException e) {
				throw new RepositoryException("Could not write readme file in folder " + CACHE_DIRECTORY.toString(), e);
			}
		}
		final Path tmpFile = CACHE_DIRECTORY.resolve("archive_" + id + ".json");
		return tmpFile;
	}

	@Override
	public void stowaway(final Archive archive) {
		final File jsonFile = getJsonPath(archive.getId()).toFile();
		try {
			mapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile, archive);
		} catch (final IOException e) {
			throw new RepositoryException("Error loading JSON file at: " + jsonFile.toString(), e);
		}
		logger.info("Cache at: " + jsonFile.toString());
	}

	@Override
	public Optional<Archive> retrieve(final ArchiveId id) {
		try {
			final Path jsonPath = getJsonPath(id);
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

}
