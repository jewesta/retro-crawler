package com.retrocrawler.demo;

import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.Clues;
import com.retrocrawler.core.archive.clues.FolderNameClueFinder;

public class SquareBracketsClueFinderThrows implements FolderNameClueFinder {

	public static final String OPENING_BRACKETS = "[";

	public static final String CLOSING_BRACKETS = "]";

	public SquareBracketsClueFinderThrows() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public Clues find(final String folderName) {
		return Clues.of(Clue.of("foo", "bar"));
	}

}
