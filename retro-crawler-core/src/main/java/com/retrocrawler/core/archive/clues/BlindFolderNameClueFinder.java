package com.retrocrawler.core.archive.clues;

/**
 * Sentinel used when folder names should not produce clues.
 */
public final class BlindFolderNameClueFinder implements FolderNameClueFinder {

	private BlindFolderNameClueFinder() {
		// Sentinel type; the framework does not instantiate it.
	}

	@Override
	public Clues find(final String folderName) {
		return Clues.none();
	}
}
