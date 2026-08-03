package com.retrocrawler.mycollection.clues;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.FileNameClueFinder;
import com.retrocrawler.mycollection.AttributeNames;

/**
 * Recognizes files whose names start with a collection {@code FD-*} image ID.
 */
public final class FloppyImageClueFinder implements FileNameClueFinder {

	private static final Pattern FILE_NAME = Pattern.compile("(?i)^(FD-\\d+)(?:\\b|[ _.-].*)");

	@Override
	public Set<Clue> find(final Collection<Path> files) {
		final Set<String> ids = new LinkedHashSet<>();
		final Set<String> paths = new LinkedHashSet<>();
		for (final Path file : files) {
			final Matcher matcher = FILE_NAME.matcher(file.getFileName().toString());
			if (matcher.matches()) {
				ids.add(matcher.group(1).toUpperCase(java.util.Locale.ROOT));
				paths.add(FileNameClueFinder.portablePath(file));
			}
		}

		final Set<Clue> clues = new LinkedHashSet<>();
		if (!ids.isEmpty()) {
			clues.add(Clue.of(AttributeNames.FLOPPY_IMAGE_ID, Set.copyOf(ids)));
			clues.add(Clue.of(AttributeNames.FLOPPY_IMAGES, Set.copyOf(paths)));
		}
		return Set.copyOf(clues);
	}
}
