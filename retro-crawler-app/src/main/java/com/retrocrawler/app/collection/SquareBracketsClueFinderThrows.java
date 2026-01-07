package com.retrocrawler.app.collection;

import java.util.Set;

import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.PathNameClueFinder;

public class SquareBracketsClueFinderThrows implements PathNameClueFinder {

	public static final String OPENING_BRACKETS = "[";

	public static final String CLOSING_BRACKETS = "]";

	public SquareBracketsClueFinderThrows() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public Set<Clue> find(final String folderName) {
		return Set.of(Clue.of("foo", "bar"));
	}

}
