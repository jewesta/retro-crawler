package com.retrocrawler.core.archive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.retrocrawler.core.archive.clues.Archive;
import com.retrocrawler.core.archive.clues.ArchiveNode;
import com.retrocrawler.core.archive.clues.ArchiveVersion;
import com.retrocrawler.core.archive.clues.Artifact;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.util.ReadmeWriter;

class JsonFileRepositoryTest {

	@TempDir
	private Path temporaryDirectory;

	@Test
	void retrievingMissingArchiveDoesNotCreateRepositoryDirectory() {
		final Path repositoryDirectory = temporaryDirectory.resolve("repository");
		final Repository repository = new JsonFileRepository(repositoryDirectory);

		assertTrue(repository.retrieve(ArchiveId.of("missing")).isEmpty());
		assertFalse(Files.exists(repositoryDirectory));
	}

	@Test
	void stowsAwayAndRetrievesArchiveInConfiguredDirectory() {
		final Path repositoryDirectory = temporaryDirectory.resolve("repository");
		final Repository repository = new JsonFileRepository(repositoryDirectory);
		final ArchiveId id = ArchiveId.of("test_archive");
		final Archive archive = archive(id, "root");

		repository.stowaway(archive);

		assertTrue(Files.exists(repositoryDirectory.resolve("archive_test_archive.json")));
		assertTrue(Files.exists(repositoryDirectory.resolve(ReadmeWriter.README_TXT)));

		final Archive retrieved = repository.retrieve(ArchiveId.of("test_archive")).orElseThrow();
		assertEquals(id, retrieved.id());
		assertEquals(temporaryDirectory.resolve("root").toString(), retrieved.basePath());
	}

	@Test
	void replacesExistingArchiveWithSameId() {
		final Repository repository = new JsonFileRepository(temporaryDirectory.resolve("repository"));
		final ArchiveId id = ArchiveId.of("replace_me");

		repository.stowaway(archive(id, "first"));
		repository.stowaway(archive(id, "second"));

		final Archive retrieved = repository.retrieve(id).orElseThrow();
		assertEquals(temporaryDirectory.resolve("second").toString(), retrieved.basePath());
	}

	@Test
	void preservesMissingValueCluesUsingEstablishedEmptyArrayFormat() throws IOException {
		final Path repositoryDirectory = temporaryDirectory.resolve("repository");
		final Repository repository = new JsonFileRepository(repositoryDirectory);
		final ArchiveId id = ArchiveId.of("missing_value");
		final Artifact artifact = new Artifact(Set.of(Clue.missingValue("sn")));
		final ArchiveNode root = new ArchiveNode("root", artifact, null);
		repository.stowaway(Archive.of(id, temporaryDirectory.resolve("root"), root));

		final JsonNode json = new ObjectMapper()
				.readTree(repositoryDirectory.resolve("archive_missing_value.json").toFile());
		final JsonNode storedClue = json.at("/root/artifact/sn");
		final Artifact retrieved = repository.retrieve(id).orElseThrow().root().artifact();
		final Clue clue = retrieved.clues().stream().findFirst().orElseThrow();

		assertEquals(ArchiveVersion.CURRENT_IMPLEMENTATION_VERSION.value().intValue(), json.path("version").asInt());
		assertTrue(storedClue.isArray());
		assertTrue(storedClue.isEmpty());
		assertEquals("sn", clue.key());
		assertTrue(clue.isMissingValue());
	}

	@Test
	void writesTheCacheVersionAsTheFirstStoredProperty() throws IOException {
		final Path repositoryDirectory = temporaryDirectory.resolve("repository");
		final Repository repository = new JsonFileRepository(repositoryDirectory);
		final ArchiveId id = ArchiveId.of("version_first");
		repository.stowaway(archive(id, "root"));

		try (JsonParser parser = new ObjectMapper().getFactory()
				.createParser(repositoryDirectory.resolve("archive_version_first.json").toFile())) {
			assertEquals(JsonToken.START_OBJECT, parser.nextToken());
			assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
			assertEquals("version", parser.currentName());
		}
	}

	@Test
	void preservesTheArchiveNodeCrawlTimestampAsReadableJson() throws IOException {
		final Instant crawledAt = Instant.parse("2026-08-05T09:42:17.123456Z");
		final Path repositoryDirectory = temporaryDirectory.resolve("repository");
		final Repository repository = new JsonFileRepository(repositoryDirectory);
		final ArchiveId id = ArchiveId.of("crawl_timestamp");
		final ArchiveNode root = new ArchiveNode("root", crawledAt, null, null);
		repository.stowaway(Archive.of(id, temporaryDirectory.resolve("root"), root));

		final JsonNode json = new ObjectMapper()
				.readTree(repositoryDirectory.resolve("archive_crawl_timestamp.json").toFile());
		final Archive retrieved = repository.retrieve(id).orElseThrow();

		assertEquals(crawledAt.toString(), json.at("/root/crawledAt").asText());
		assertEquals(crawledAt, retrieved.root().crawledAt());
	}

	@Test
	void encodesArchiveIdForUseAsFileName() throws IOException {
		final Path repositoryDirectory = temporaryDirectory.resolve("repository");
		final Repository repository = new JsonFileRepository(repositoryDirectory);

		repository.stowaway(archive(ArchiveId.of("../../retro gear"), "root"));

		try (Stream<Path> files = Files.list(repositoryDirectory)) {
			assertEquals(1, files.filter(path -> path.toString().endsWith(".json")).count());
		}
		assertFalse(Files.exists(temporaryDirectory.resolve("retro gear.json")));
	}

	@Test
	void reportsInvalidJsonAsRepositoryFailure() throws IOException {
		final Path repositoryDirectory = temporaryDirectory.resolve("repository");
		Files.createDirectories(repositoryDirectory);
		Files.writeString(repositoryDirectory.resolve("archive_broken.json"), "This is not JSON.");
		final Repository repository = new JsonFileRepository(repositoryDirectory);

		assertThrows(RepositoryException.class, () -> repository.retrieve(ArchiveId.of("broken")));
	}

	@Test
	void reportsEmptyJsonValueAsRepositoryFailure() throws IOException {
		final Path repositoryDirectory = temporaryDirectory.resolve("repository");
		Files.createDirectories(repositoryDirectory);
		Files.writeString(repositoryDirectory.resolve("archive_empty.json"), "null");
		final Repository repository = new JsonFileRepository(repositoryDirectory);

		assertThrows(RepositoryException.class, () -> repository.retrieve(ArchiveId.of("empty")));
	}

	@Test
	void rejectsArchiveWhoseStoredIdDoesNotMatchRequestedId() throws IOException {
		final Path repositoryDirectory = temporaryDirectory.resolve("repository");
		final Repository repository = new JsonFileRepository(repositoryDirectory);
		repository.stowaway(archive(ArchiveId.of("actual"), "root"));
		Files.move(repositoryDirectory.resolve("archive_actual.json"),
				repositoryDirectory.resolve("archive_requested.json"));

		assertThrows(RepositoryException.class, () -> repository.retrieve(ArchiveId.of("requested")));
	}

	@Test
	void rejectsAnOlderShapeBeforeDeserializingItAsTheCurrentArchive() throws IOException {
		final Path repositoryDirectory = temporaryDirectory.resolve("repository");
		final Repository repository = new JsonFileRepository(repositoryDirectory);
		final ArchiveId id = ArchiveId.of("old_paths");
		repository.stowaway(archive(id, "root"));
		final Path jsonPath = repositoryDirectory.resolve("archive_old_paths.json");
		final ObjectMapper mapper = new ObjectMapper();
		final ObjectNode json = (ObjectNode) mapper.readTree(jsonPath.toFile());
		json.put("version", 2);
		json.remove("root");
		json.putArray("buckets");
		mapper.writeValue(jsonPath.toFile(), json);

		final RepositoryException failure = assertThrows(RepositoryException.class, () -> repository.retrieve(id));

		assertTrue(failure.getMessage().contains("uses cache version 2"));
	}

	@Test
	void rejectsThePreviousCacheVersionEvenWhenItsShapeCanStillBeDecoded() throws IOException {
		final Path repositoryDirectory = temporaryDirectory.resolve("repository");
		final Repository repository = new JsonFileRepository(repositoryDirectory);
		final ArchiveId id = ArchiveId.of("old_node_timestamps");
		repository.stowaway(archive(id, "root"));
		final Path jsonPath = repositoryDirectory.resolve("archive_old_node_timestamps.json");
		final ObjectMapper mapper = new ObjectMapper();
		final ObjectNode json = (ObjectNode) mapper.readTree(jsonPath.toFile());
		json.put("version", 4);
		mapper.writeValue(jsonPath.toFile(), json);

		final RepositoryException failure = assertThrows(RepositoryException.class, () -> repository.retrieve(id));

		assertTrue(failure.getMessage().contains("uses cache version 4"));
	}

	@Test
	void rejectsAFutureVersionBeforeDeserializingIt() throws IOException {
		final Path repositoryDirectory = temporaryDirectory.resolve("repository");
		Files.createDirectories(repositoryDirectory);
		final Path jsonPath = repositoryDirectory.resolve("archive_future.json");
		Files.writeString(jsonPath, "{\"version\":99,\"notAnArchive\":true}");
		final Repository repository = new JsonFileRepository(repositoryDirectory);

		final RepositoryException failure = assertThrows(RepositoryException.class,
				() -> repository.retrieve(ArchiveId.of("future")));

		assertTrue(failure.getMessage().contains("uses cache version 99"));
	}

	@Test
	void rejectsAMissingOrMalformedVersion() throws IOException {
		final Path repositoryDirectory = temporaryDirectory.resolve("repository");
		Files.createDirectories(repositoryDirectory);
		Files.writeString(repositoryDirectory.resolve("archive_missing_version.json"), "{\"id\":\"missing_version\"}");
		Files.writeString(repositoryDirectory.resolve("archive_text_version.json"),
				"{\"version\":\"5\",\"id\":\"text_version\"}");
		final Repository repository = new JsonFileRepository(repositoryDirectory);

		assertThrows(RepositoryException.class, () -> repository.retrieve(ArchiveId.of("missing_version")));
		assertThrows(RepositoryException.class, () -> repository.retrieve(ArchiveId.of("text_version")));
	}

	private Archive archive(final ArchiveId id, final String folder) {
		final ArchiveNode root = new ArchiveNode(folder, null, null);
		return Archive.of(id, temporaryDirectory.resolve(folder), root);
	}

}
