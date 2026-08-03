package com.retrocrawler.core.archive.clues;

import java.util.Set;

/**
 * Sentinel used when folder names should not produce clues.
 */
public final class BlindFolderNameClueFinder implements FolderNameClueFinder {

	private BlindFolderNameClueFinder() {
		// Sentinel type; the framework does not instantiate it.
	}

	@Override
	public Set<Clue> find(final String folderName) {
		return Set.of();
	}
}
