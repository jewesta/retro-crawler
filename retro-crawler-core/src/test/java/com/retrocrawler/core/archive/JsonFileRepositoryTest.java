package com.retrocrawler.core.archive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retrocrawler.core.archive.clues.Archive;
import com.retrocrawler.core.archive.clues.ArchiveNode;
import com.retrocrawler.core.archive.clues.Artifact;
import com.retrocrawler.core.archive.clues.Bucket;
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
		assertEquals(id, retrieved.getId());
		assertEquals(1, retrieved.getBuckets().size());
		assertEquals(temporaryDirectory.resolve("root").toString(), retrieved.getBuckets().get(0).getBasePath());
	}

	@Test
	void replacesExistingArchiveWithSameId() {
		final Repository repository = new JsonFileRepository(temporaryDirectory.resolve("repository"));
		final ArchiveId id = ArchiveId.of("replace_me");

		repository.stowaway(archive(id, "first"));
		repository.stowaway(archive(id, "second"));

		final Archive retrieved = repository.retrieve(id).orElseThrow();
		assertEquals(temporaryDirectory.resolve("second").toString(), retrieved.getBuckets().get(0).getBasePath());
	}

	@Test
	void preservesMissingValueCluesUsingEstablishedEmptyArrayFormat() throws IOException {
		final Path repositoryDirectory = temporaryDirectory.resolve("repository");
		final Repository repository = new JsonFileRepository(repositoryDirectory);
		final ArchiveId id = ArchiveId.of("missing_value");
		final Artifact artifact = new Artifact(Set.of(Clue.missingValue("sn")));
		final ArchiveNode root = new ArchiveNode("root", artifact, null);
		repository.stowaway(Archive.of(id, List.of(Bucket.of(temporaryDirectory.resolve("root"), root))));

		final JsonNode json = new ObjectMapper()
				.readTree(repositoryDirectory.resolve("archive_missing_value.json").toFile());
		final JsonNode storedClue = json.at("/buckets/0/root/artifact/sn");
		final Artifact retrieved = repository.retrieve(id).orElseThrow().getBuckets().getFirst().getRoot()
				.getArtifact();
		final Clue clue = retrieved.getClues().stream().findFirst().orElseThrow();

		assertEquals(1, json.path("version").asInt());
		assertTrue(storedClue.isArray());
		assertTrue(storedClue.isEmpty());
		assertEquals("sn", clue.getKey());
		assertTrue(clue.isMissingValue());
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

	private Archive archive(final ArchiveId id, final String folder) {
		final ArchiveNode root = new ArchiveNode(folder, null, null);
		final Bucket bucket = Bucket.of(temporaryDirectory.resolve(folder), root);
		return Archive.of(id, List.of(bucket));
	}

}
