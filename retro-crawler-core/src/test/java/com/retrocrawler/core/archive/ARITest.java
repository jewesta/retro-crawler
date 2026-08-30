package com.retrocrawler.core.archive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ARITest {

	@Test
	void identifiesACollectionQualifiedArchiveResourceAsAUri() {
		final ARI ari = ARI.of("retro collection", ArchiveId.of("incoming/material"),
				Path.of("Graphics Cards", "Voodoo #3"));

		assertEquals("ari:/retro%20collection/incoming%2Fmaterial/Graphics%20Cards/Voodoo%20%233", ari.toString());
		assertEquals("retro collection", ari.collectionId());
		assertEquals(ArchiveId.of("incoming/material"), ari.archiveId());
		assertEquals(Path.of("Graphics Cards", "Voodoo #3"), ari.resourcePath());
	}

	@Test
	void parsesItsUriRepresentation() {
		final ARI expected = ARI.of("collection", ArchiveId.of("archive"), Path.of("folder", "file.txt"));

		assertEquals(expected, ARI.parse(expected.toString()));
		assertEquals(expected, ARI.from(URI.create(expected.toString())));
		assertEquals(Path.of("a+b"), ARI.parse("ari:/collection/archive/a+b").resourcePath());
	}

	@Test
	void identifiesTheArchiveRootWithAnEmptyResourcePath() {
		final ARI ari = ARI.of("collection", ArchiveId.of("archive"), Path.of(""));

		assertEquals("ari:/collection/archive", ari.toString());
		assertEquals(Path.of(""), ari.resourcePath());
	}

	@Test
	void resolvesAResourceBelowItsCurrentAddress() {
		final ARI artifact = ARI.of("collection", ArchiveId.of("archive"), Path.of("shelf/gear"));

		assertEquals(ARI.of("collection", ArchiveId.of("archive"), Path.of("shelf/gear/Box/front.jpeg")),
				artifact.resolve(Path.of("Box/front.jpeg")));
	}

	@Test
	void rejectsPhysicalAndTraversalPathsWhenResolving() {
		final ARI artifact = ARI.of("collection", ArchiveId.of("archive"), Path.of("gear"));

		assertThrows(IllegalArgumentException.class, () -> artifact.resolve(Path.of("/physical/path")));
		assertThrows(IllegalArgumentException.class, () -> artifact.resolve(Path.of("../outside")));
		assertThrows(IllegalArgumentException.class, () -> artifact.resolve(Path.of("inside/../outside")));
	}

	@Test
	void rejectsPhysicalAndEscapingResourcePaths() {
		assertThrows(IllegalArgumentException.class,
				() -> ARI.of("collection", ArchiveId.of("archive"), Path.of("/physical/path")));
		assertThrows(IllegalArgumentException.class,
				() -> ARI.of("collection", ArchiveId.of("archive"), Path.of("../outside")));
		assertThrows(IllegalArgumentException.class,
				() -> ARI.of("collection", ArchiveId.of("archive"), Path.of("inside/../outside")));
	}

	@Test
	void rejectsUrisOutsideTheAriShape() {
		assertThrows(IllegalArgumentException.class, () -> ARI.parse("https://example.com/resource"));
		assertThrows(IllegalArgumentException.class, () -> ARI.parse("ari:/collection"));
		assertThrows(IllegalArgumentException.class, () -> ARI.parse("ari:/collection/archive/../outside"));
		assertThrows(IllegalArgumentException.class, () -> ARI.parse("ari:/collection/archive/inside%2Foutside"));
	}
}
