package com.retrocrawler.model;

import java.nio.file.Path;

import com.retrocrawler.core.Configuration;
import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.gear.parser.ParseContext;

public final class ParserTestContext {

	public static final ParseContext CONTEXT = new ParseContext(Configuration.builder().build(),
			ARI.of("test", ArchiveId.of("archive"), Path.of("gear")));

	private ParserTestContext() {
		// test utility class
	}
}
