package com.retrocrawler.mycollection.clues;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.retrocrawler.core.archive.clues.ArchiveFolderView;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.ClueAccumulator;
import com.retrocrawler.core.archive.clues.ClueFinder;
import com.retrocrawler.core.archive.clues.Clues;
import com.retrocrawler.mycollection.AttributeNames;

/**
 * Recognizes the collection's three conventional gear photographs.
 */
public final class StandardImageClueFinder implements ClueFinder {

	private static final Map<String, String> KEYS_BY_FILE_NAME = Map.of("angled.jpeg", AttributeNames.IMAGE_ANGLED,
			"front.jpeg", AttributeNames.IMAGE_FRONT, "back.jpeg", AttributeNames.IMAGE_BACK);

	@Override
	public Clues find(final ArchiveFolderView folder) {
		final Map<String, Set<String>> valuesByKey = new java.util.LinkedHashMap<>();
		for (final var file : folder.files()) {
			final String fileName = file.name().toLowerCase(Locale.ROOT);
			final String key = KEYS_BY_FILE_NAME.get(fileName);
			if (key != null) {
				valuesByKey.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).add(file.name());
			}
		}

		final ClueAccumulator clues = Clues.accumulator();
		valuesByKey.forEach((key, values) -> clues.add(Clue.of(key, Set.copyOf(values))));
		return clues.clues();
	}
}
