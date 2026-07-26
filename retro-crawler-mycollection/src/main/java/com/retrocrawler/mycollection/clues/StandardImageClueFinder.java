package com.retrocrawler.mycollection.clues;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.FileNameClueFinder;
import com.retrocrawler.mycollection.AttributeNames;

/**
 * Recognizes the collection's three conventional gear photographs.
 */
public final class StandardImageClueFinder implements FileNameClueFinder {

	private static final Map<String, String> KEYS_BY_FILE_NAME = Map.of(
			"angled.jpeg", AttributeNames.IMAGE_ANGLED,
			"front.jpeg", AttributeNames.IMAGE_FRONT,
			"back.jpeg", AttributeNames.IMAGE_BACK);

	@Override
	public Set<Clue> find(final Collection<Path> files) {
		final Map<String, Set<String>> valuesByKey = new java.util.LinkedHashMap<>();
		for (final Path file : files) {
			final String fileName = file.getFileName().toString().toLowerCase(Locale.ROOT);
			final String key = KEYS_BY_FILE_NAME.get(fileName);
			if (key != null) {
				valuesByKey.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).add(file.toString());
			}
		}

		final Set<Clue> clues = new LinkedHashSet<>();
		valuesByKey.forEach((key, values) -> clues.add(Clue.of(key, Set.copyOf(values))));
		return Set.copyOf(clues);
	}
}
