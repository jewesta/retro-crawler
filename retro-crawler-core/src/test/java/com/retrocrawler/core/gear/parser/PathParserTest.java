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
	void bindsARelativePathToTheCurrentlyConfiguredArchiveRoot() {
		final Path root = Path.of("/mounted/archive");
		final FactParseContext context = FactParseContext.located(root, root.resolve("shelf/gear"));

		final RatedFact<Path> fact = parser.parse("shelf/gear/front.jpeg", context);

		assertEquals(Confidence.EXACT, fact.confidence());
		assertEquals(root.resolve("shelf/gear/front.jpeg"), fact.value().orElseThrow());
	}

	@Test
	void rejectsAbsoluteAndEscapingPaths() {
		final Path root = Path.of("/mounted/archive");
		final FactParseContext context = FactParseContext.located(root, root.resolve("shelf/gear"));

		assertTrue(parser.parse("/outside/front.jpeg", context).value().isEmpty());
		assertTrue(parser.parse("../outside/front.jpeg", context).value().isEmpty());
	}
}
