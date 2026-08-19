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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
	private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

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
			final ObjectNode stored = mapper.valueToTree(archive);
			ArchiveJsonSources.compact(stored);
			mapper.writerWithDefaultPrettyPrinter().writeValue(temporaryPath.toFile(), stored);
			moveIntoPlace(temporaryPath, jsonPath);
		} catch (final IOException | RuntimeException e) {
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
			final int version = inspectVersion(jsonFile, jsonPath);
			if (version != ArchiveVersion.CURRENT_IMPLEMENTATION_VERSION.value().intValue()) {
				throw unsupportedVersion(jsonPath, version);
			}
			final JsonNode stored = mapper.readTree(jsonFile);
			ArchiveJsonSources.expand(stored);
			final Archive archive = mapper.treeToValue(stored, Archive.class);
			if (archive == null) {
				throw new RepositoryException("Stored JSON does not contain an archive at: " + jsonPath);
			}
			if (!id.equals(archive.id())) {
				throw new RepositoryException(
						"Expected archive id '" + id + "' but retrieved '" + archive.id() + "' from: " + jsonPath);
			}
			if (!ArchiveVersion.CURRENT_IMPLEMENTATION_VERSION.equals(archive.version())) {
				throw unsupportedVersion(jsonPath, archive.version().value().intValue());
			}
			return Optional.of(archive);
		} catch (final RepositoryException e) {
			throw e;
		} catch (final IOException | RuntimeException e) {
			throw new RepositoryException("Could not retrieve archive from JSON at: " + jsonPath, e);
		}
	}

	private int inspectVersion(final File jsonFile, final Path jsonPath) throws IOException {
		try (JsonParser parser = mapper.getFactory().createParser(jsonFile)) {
			if (parser.nextToken() != JsonToken.START_OBJECT) {
				throw new RepositoryException("Stored JSON does not contain an archive object at: " + jsonPath);
			}
			JsonToken token;
			while ((token = parser.nextToken()) != JsonToken.END_OBJECT) {
				if (token == null) {
					throw new RepositoryException("Stored JSON archive object is incomplete at: " + jsonPath);
				}
				if (token != JsonToken.FIELD_NAME) {
					throw new RepositoryException("Stored JSON archive object is malformed at: " + jsonPath);
				}
				final String fieldName = parser.currentName();
				final JsonToken value = parser.nextToken();
				if (value == null) {
					throw new RepositoryException("Stored JSON archive object is incomplete at: " + jsonPath);
				}
				if ("version".equals(fieldName)) {
					if (value != JsonToken.VALUE_NUMBER_INT) {
						throw new RepositoryException("Stored archive has a non-integer cache version at: " + jsonPath);
					}
					return parser.getIntValue();
				}
				parser.skipChildren();
			}
		}
		throw new RepositoryException("Stored archive has no cache version at: " + jsonPath);
	}

	private static RepositoryException unsupportedVersion(final Path jsonPath, final int version) {
		return new RepositoryException("Stored archive at " + jsonPath + " uses cache version " + version
				+ " but this crawler requires " + ArchiveVersion.CURRENT_IMPLEMENTATION_VERSION + ".");
	}

}
