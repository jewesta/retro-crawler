package com.retrocrawler.core.archive.clues;

import java.util.Set;

/**
 * Default path name clue finder that will always find nothing.
 */
public final class BlindPathNameClueFinder implements PathNameClueFinder {

	private BlindPathNameClueFinder() {
	}

	@Override
	public Set<Clue> find(final String folderName) {
		return Set.of();
	}

}
