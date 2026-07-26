package com.retrocrawler.mycollection.clues;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.FileNameClueFinder;
import com.retrocrawler.mycollection.AttributeNames;

/**
 * Retains macOS {@code .webloc} and Windows {@code .url} reference files.
 */
public final class WebReferenceClueFinder implements FileNameClueFinder {

	@Override
	public Set<Clue> find(final Collection<Path> files) {
		final Set<String> references = new LinkedHashSet<>();
		for (final Path file : files) {
			final String fileName = file.getFileName().toString().toLowerCase(Locale.ROOT);
			if (fileName.endsWith(".webloc") || fileName.endsWith(".url")) {
				references.add(file.toString());
			}
		}
		return references.isEmpty() ? Set.of()
				: Set.of(Clue.of(AttributeNames.WEB_REFERENCES, Set.copyOf(references)));
	}
}
