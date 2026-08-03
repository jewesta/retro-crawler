package com.retrocrawler.core.discovery.fixture;

import java.util.Set;

import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.FolderNameClueFinder;

public class EmptyClueFinder implements FolderNameClueFinder {

	@Override
	public Set<Clue> find(final String folderName) {
		return Set.of();
	}
}
