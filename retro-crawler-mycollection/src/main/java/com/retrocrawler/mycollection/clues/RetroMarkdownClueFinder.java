package com.retrocrawler.mycollection.clues;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.ClueFileIOException;
import com.retrocrawler.core.archive.clues.FileContentClueFinder;
import com.retrocrawler.mycollection.AttributeNames;

/**
 * Reads the complete UTF-8 contents of a {@code retro.md} collection note.
 */
public final class RetroMarkdownClueFinder implements FileContentClueFinder {

	private static final String FILE_NAME = "retro.md";

	@Override
	public boolean matches(final String fileName) {
		return FILE_NAME.equalsIgnoreCase(fileName);
	}

	@Override
	public Set<Clue> find(final InputStream is) {
		try {
			return Set.of(Clue.of(AttributeNames.DESCRIPTION, new String(is.readAllBytes(), StandardCharsets.UTF_8)));
		} catch (final IOException e) {
			throw new ClueFileIOException("Could not read " + FILE_NAME + ".", e);
		}
	}
}
