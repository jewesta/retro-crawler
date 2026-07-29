package com.retrocrawler.mycollection.clues;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.ClueFileIOException;
import com.retrocrawler.core.archive.clues.FileContentClueFinder;

/**
 * Imports the collection's legacy {@code retro.properties} metadata.
 */
public final class RetroPropertiesClueFinder implements FileContentClueFinder {

	private static final String FILE_NAME = "retro.properties";

	@Override
	public boolean matches(final String fileName) {
		return FILE_NAME.equalsIgnoreCase(fileName);
	}

	@Override
	public Set<Clue> find(final InputStream is) {
		final Properties properties = new Properties();
		try {
			properties.load(new InputStreamReader(is, StandardCharsets.UTF_8));
		} catch (final IOException e) {
			throw new ClueFileIOException("Could not read " + FILE_NAME + ".", e);
		}

		final Set<Clue> clues = new LinkedHashSet<>();
		properties.stringPropertyNames().stream().sorted()
				.forEach(key -> clues.add(Clue.of(key, properties.getProperty(key))));
		return Set.copyOf(clues);
	}
}
