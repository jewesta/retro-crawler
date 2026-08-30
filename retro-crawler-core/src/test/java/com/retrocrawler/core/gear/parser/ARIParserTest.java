package com.retrocrawler.core.gear.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.Configuration;
import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.gear.Confidence;
import com.retrocrawler.core.gear.RatedFact;

class ARIParserTest {

	private static final ARI SOURCE = ARI.of("test", ArchiveId.of("archive"), Path.of("shelf/gear"));
	private static final ParseContext CONTEXT = new ParseContext(Configuration.builder().build(), SOURCE);

	private final ARIParser parser = new ARIParser();

	@Test
	void bindsARelativeResourcePathToTheCurrentArtifact() {
		final RatedFact<ARI> fact = parser.parse("front.jpeg", CONTEXT);

		assertEquals(Confidence.EXACT, fact.confidence());
		assertEquals(ARI.of("test", ArchiveId.of("archive"), Path.of("shelf/gear/front.jpeg")),
				fact.value().orElseThrow());
	}

	@Test
	void preservesAResourcePathBelowTheArtifact() {
		final RatedFact<ARI> fact = parser.parse("Box/front.jpeg", CONTEXT);

		assertEquals(ARI.of("test", ArchiveId.of("archive"), Path.of("shelf/gear/Box/front.jpeg")),
				fact.value().orElseThrow());
	}

	@Test
	void rejectsPhysicalAndEscapingPaths() {
		assertTrue(parser.parse("/outside/front.jpeg", CONTEXT).value().isEmpty());
		assertTrue(parser.parse("../outside/front.jpeg", CONTEXT).value().isEmpty());
		assertTrue(parser.parse("", CONTEXT).value().isEmpty());
	}
}
