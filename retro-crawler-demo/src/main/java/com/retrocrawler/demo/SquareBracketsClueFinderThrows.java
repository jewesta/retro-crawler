package com.retrocrawler.demo;

import com.retrocrawler.core.archive.clues.ArchiveFolderView;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.ClueFinder;
import com.retrocrawler.core.archive.clues.Clues;

public class SquareBracketsClueFinderThrows implements ClueFinder {

	public static final String OPENING_BRACKETS = "[";

	public static final String CLOSING_BRACKETS = "]";

	public SquareBracketsClueFinderThrows() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public Clues find(final ArchiveFolderView folder) {
		return Clues.of(Clue.of("foo", "bar"));
	}

}
