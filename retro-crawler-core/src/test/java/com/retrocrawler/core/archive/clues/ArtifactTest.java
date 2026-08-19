package com.retrocrawler.core.archive.clues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.archive.ArchiveId;

class ArtifactTest {

	@Test
	void protectsCachedCluesFromExternalMutation() {
		final List<Clue> observed = new java.util.ArrayList<>(List.of(Clue.of("bus", "AGP")));
		final Artifact artifact = new Artifact(Clues.of(observed));

		observed.clear();

		assertEquals(1, artifact.clues().size());
	}

	@Test
	void protectsRawClueValuesFromExternalMutation() {
		final Set<String> values = new HashSet<>(Set.of("AGP"));
		final Clue clue = Clue.of("bus", values);

		values.add("PCI");

		assertEquals(Set.of("AGP"), clue.value());
		assertThrows(UnsupportedOperationException.class, () -> clue.value().add("PCI"));
	}

	@Test
	void holdsTheCluesItWasGivenWithoutRebuildingThem() {
		final Clues clues = Clues.of(Clue.of("bus", "AGP"));

		assertSame(clues, new Artifact(clues).clues());
	}

	@Test
	void rejectsAnArtifactWithoutAClue() {
		assertThrows(IllegalArgumentException.class, () -> new Artifact(Clues.none()));
		assertThrows(NullPointerException.class, () -> new Artifact(null));
	}

	@Test
	void persistsFinderAndExplicitSourcesWhileOmittingAbsentProvenance() throws Exception {
		final ARI image = ARI.of("test_collection", ArchiveId.of("photos"), Path.of("Board", "front.jpeg"));
		final Clue sourced = Clue.of("image", "front.jpeg").from(image).foundBy("ImageClueFinder");
		final Clue unsourced = Clue.of("description", "Notes").foundBy("RetroMarkdownClueFinder");
		final Clue internal = Clue.internal(InternalClueKeys.ID, "technical-id");
		final ObjectMapper mapper = new ObjectMapper();

		final String json = mapper.writeValueAsString(new Artifact(Clues.of(sourced, unsourced, internal)));
		final JsonNode stored = mapper.readTree(json);
		final Artifact retrieved = mapper.readValue(json, Artifact.class);

		assertEquals("ImageClueFinder", stored.at("/image/finder").asText());
		assertEquals(image.toString(), stored.at("/image/sources/0").asText());
		assertEquals("RetroMarkdownClueFinder", stored.at("/description/finder").asText());
		assertTrue(stored.at("/description/sources").isMissingNode());
		assertTrue(stored.at("/" + InternalClueKeys.ID + "/finder").isMissingNode());
		assertTrue(stored.at("/" + InternalClueKeys.ID + "/sources").isMissingNode());

		final Clue retrievedSource = retrieved.clues().get("image").orElseThrow();
		assertEquals("ImageClueFinder", retrievedSource.finder().orElseThrow());
		assertEquals(List.of(image), retrievedSource.sources());
		assertTrue(retrieved.clues().get(InternalClueKeys.ID).orElseThrow().finder().isEmpty());
	}
}
