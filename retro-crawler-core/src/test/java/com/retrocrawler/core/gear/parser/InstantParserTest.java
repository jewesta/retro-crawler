package com.retrocrawler.core.gear.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.Configuration;
import com.retrocrawler.core.archive.Node;
import com.retrocrawler.core.archive.clues.Confidence;

class InstantParserTest {

	private static final Path ARCHIVE_ROOT = Path.of("/archive");
	private static final ParseContext CONTEXT = new ParseContext(Configuration.builder().build(),
			new Node(ARCHIVE_ROOT, ARCHIVE_ROOT.resolve("gear")));

	private final InstantParser parser = new InstantParser();

	@Test
	void parsesIsoInstantsAndOffsetDateTimes() {
		assertEquals(Instant.parse("2024-01-30T13:35:00Z"),
				parser.parse("2024-01-30T13:35:00Z", CONTEXT).value().orElseThrow());
		assertEquals(Instant.parse("2024-01-30T13:35:00Z"),
				parser.parse("2024-01-30T14:35:00+01:00", CONTEXT).value().orElseThrow());
		assertEquals(Instant.parse("2024-01-30T13:35:00.123456Z"),
				parser.parse(" 2024-01-30T13:35:00.123456Z ", CONTEXT).value().orElseThrow());
	}

	@Test
	void parsesIntegerValuesAsEpochMilliseconds() {
		final Instant expected = Instant.parse("2024-01-30T13:35:00Z");

		assertEquals(expected, parser.parse(Long.toString(expected.toEpochMilli()), CONTEXT).value().orElseThrow());
	}

	@Test
	void rejectsValuesWithoutAnOffsetAndInvalidValues() {
		assertEquals(Confidence.NONE, parser.parse("2024-01-30T13:35:00", CONTEXT).confidence());
		assertEquals(Confidence.NONE, parser.parse("2024-01-30", CONTEXT).confidence());
		assertEquals(Confidence.NONE, parser.parse("not an instant", CONTEXT).confidence());
		assertTrue(parser.parse(null, CONTEXT).value().isEmpty());
	}
}
