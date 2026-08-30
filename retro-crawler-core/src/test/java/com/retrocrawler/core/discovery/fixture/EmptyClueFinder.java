package com.retrocrawler.core.discovery.fixture;

import com.retrocrawler.core.archive.clues.ArchiveFolderView;
import com.retrocrawler.core.archive.clues.ClueFinder;
import com.retrocrawler.core.archive.clues.Clues;

public class EmptyClueFinder implements ClueFinder {

	@Override
	public Clues find(final ArchiveFolderView folder) {
		return Clues.none();
	}
}
