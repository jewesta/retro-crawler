package com.retrocrawler.core.archive;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retrocrawler.core.archive.clues.Archive;
import com.retrocrawler.core.archive.clues.ArchiveVersion;
import com.retrocrawler.core.util.ReadmeWriter;

/**
 * Stores one JSON file per extracted archive in a local directory.
 */
public class JsonFileRepository implements Repository {

	public static final Path DEFAULT_DIRECTORY = Path.of("cache");

	private static final Logger logger = LoggerFactory.getLogger(JsonFileRepository.class);

	private final Path directory;

	// Thread-safe after configuration, as recommended by the ObjectMapper javadoc.
	private final ObjectMapper mapper = new ObjectMapper();

	public JsonFileRepository() {
		this(DEFAULT_DIRECTORY);
	}

	public JsonFileRepository(final Path directory) {
		this.directory = Objects.requireNonNull(directory, "directory");
	}

	private Path jsonPath(final ArchiveId id) {
		Objects.requireNonNull(id, "id");
		final String encodedId = URLEncoder.encode(id.value(), StandardCharsets.UTF_8);
		return directory.resolve("archive_" + encodedId + ".json");
	}

	private void prepareDirectory() {
		final boolean createReadme = Files.notExists(directory);
		try {
			Files.createDirectories(directory);
			if (createReadme) {
				ReadmeWriter.writeReadmeTemporary(directory);
			}
		} catch (final IOException e) {
			throw new RepositoryException("Could not prepare JSON repository directory at: " + directory, e);
		}
	}

	@Override
	public void stowaway(final Archive archive) {
		Objects.requireNonNull(archive, "archive");
		prepareDirectory();

		final Path jsonPath = jsonPath(archive.id());
		Path temporaryPath = null;
		try {
			temporaryPath = Files.createTempFile(directory, ".retro-crawler-", ".json");
			mapper.writerWithDefaultPrettyPrinter().writeValue(temporaryPath.toFile(), archive);
			moveIntoPlace(temporaryPath, jsonPath);
		} catch (final IOException e) {
			throw new RepositoryException("Could not stow away archive as JSON at: " + jsonPath, e);
		} finally {
			deleteTemporaryFile(temporaryPath);
		}
		logger.info("Archive stowed away at: {}", jsonPath);
	}

	private static void moveIntoPlace(final Path source, final Path target) throws IOException {
		try {
			Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} catch (final AtomicMoveNotSupportedException e) {
			Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static void deleteTemporaryFile(final Path temporaryPath) {
		if (temporaryPath == null) {
			return;
		}
		try {
			Files.deleteIfExists(temporaryPath);
		} catch (final IOException e) {
			logger.warn("Could not delete temporary repository file at: {}", temporaryPath, e);
		}
	}

	@Override
	public Optional<Archive> retrieve(final ArchiveId id) {
		Objects.requireNonNull(id, "id");
		final Path jsonPath = jsonPath(id);
		if (Files.notExists(jsonPath)) {
			return Optional.empty();
		}

		try {
			final File jsonFile = jsonPath.toFile();
			logger.info("Retrieving archive from: {}", jsonPath);
			final Archive archive = mapper.readValue(jsonFile, Archive.class);
			if (archive == null) {
				throw new RepositoryException("Stored JSON does not contain an archive at: " + jsonPath);
			}
			if (!id.equals(archive.id())) {
				throw new RepositoryException(
						"Expected archive id '" + id + "' but retrieved '" + archive.id() + "' from: " + jsonPath);
			}
			if (!ArchiveVersion.CURRENT_IMPLEMENTATION_VERSION.equals(archive.version())) {
				throw new RepositoryException("Stored archive at " + jsonPath + " uses cache version "
						+ archive.version() + " but this crawler requires "
						+ ArchiveVersion.CURRENT_IMPLEMENTATION_VERSION + ".");
			}
			return Optional.of(archive);
		} catch (final IOException e) {
			throw new RepositoryException("Could not retrieve archive from JSON at: " + jsonPath, e);
		}
	}

}
