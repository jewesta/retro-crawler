package com.retrocrawler.core.gear.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Confidence;
import com.retrocrawler.core.gear.RatedFact;

class PathParserTest {

	private final PathParser parser = new PathParser();

	@Test
	void retainsARelativePathDuringDetachedResolution() {
		final RatedFact fact = parser.parse("shelf/gear/front.jpeg");

		assertEquals(Confidence.EXACT, fact.getConfidence());
		assertEquals(Path.of("shelf/gear/front.jpeg"), fact.getValue().orElseThrow());
	}

	@Test
	void bindsARelativePathToTheCurrentlyConfiguredArchiveRoot() {
		final Path root = Path.of("/mounted/archive");
		final FactParseContext context = FactParseContext.located(root, root.resolve("shelf/gear"));

		final RatedFact fact = parser.parse("shelf/gear/front.jpeg", context);

		assertEquals(Confidence.EXACT, fact.getConfidence());
		assertEquals(root.resolve("shelf/gear/front.jpeg"), fact.getValue().orElseThrow());
	}

	@Test
	void rejectsAbsoluteAndEscapingPaths() {
		assertTrue(parser.parse("/outside/front.jpeg").getValue().isEmpty());
		assertTrue(parser.parse("../outside/front.jpeg").getValue().isEmpty());
	}
}
