package com.retrocrawler.model;

import java.nio.file.Path;

import com.retrocrawler.core.Configuration;
import com.retrocrawler.core.archive.ArtifactLocation;
import com.retrocrawler.core.gear.parser.ParseContext;

public final class ParserTestContext {

	private static final Path ARCHIVE_ROOT = Path.of("/archive");

	public static final ParseContext CONTEXT = new ParseContext(Configuration.builder().build(),
			new ArtifactLocation(ARCHIVE_ROOT, ARCHIVE_ROOT.resolve("gear")));

	private ParserTestContext() {
		// test utility class
	}
}
