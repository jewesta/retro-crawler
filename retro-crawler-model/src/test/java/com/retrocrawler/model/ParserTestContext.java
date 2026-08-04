package com.retrocrawler.model;

import java.nio.file.Path;

import com.retrocrawler.core.gear.parser.FactParseContext;

public final class ParserTestContext {

	private static final Path ARCHIVE_ROOT = Path.of("/archive");

	public static final FactParseContext CONTEXT = FactParseContext.located(ARCHIVE_ROOT,
			ARCHIVE_ROOT.resolve("gear"));

	private ParserTestContext() {
		// test utility class
	}
}
