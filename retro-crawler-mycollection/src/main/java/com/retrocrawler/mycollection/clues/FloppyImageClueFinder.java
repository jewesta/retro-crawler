package com.retrocrawler.mycollection.clues;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.retrocrawler.core.archive.clues.ArchiveFileView;
import com.retrocrawler.core.archive.clues.ArchiveFolderView;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.ClueFinder;
import com.retrocrawler.core.archive.clues.Clues;
import com.retrocrawler.mycollection.AttributeNames;

/**
 * Recognizes files whose names start with a collection {@code FD-*} image ID.
 */
public final class FloppyImageClueFinder implements ClueFinder {

	private static final Pattern FILE_NAME = Pattern.compile("(?i)^(FD-\\d+)(?:\\b|[ _.-].*)");

	@Override
	public Clues find(final ArchiveFolderView folder) {
		final Set<String> ids = new LinkedHashSet<>();
		final Set<String> paths = new LinkedHashSet<>();
		final java.util.List<ArchiveFileView> sources = new java.util.ArrayList<>();
		for (final var file : folder.files()) {
			final Matcher matcher = FILE_NAME.matcher(file.name());
			if (matcher.matches()) {
				ids.add(matcher.group(1).toUpperCase(java.util.Locale.ROOT));
				paths.add(file.name());
				sources.add(file);
			}
		}

		if (ids.isEmpty()) {
			return Clues.none();
		}
		Clue idClue = Clue.of(AttributeNames.FLOPPY_IMAGE_ID, Set.copyOf(ids));
		Clue imageClue = Clue.of(AttributeNames.FLOPPY_IMAGES, Set.copyOf(paths));
		for (final ArchiveFileView source : sources) {
			idClue = idClue.from(source.ari());
			imageClue = imageClue.from(source.ari());
		}
		return Clues.of(idClue, imageClue);
	}
}
