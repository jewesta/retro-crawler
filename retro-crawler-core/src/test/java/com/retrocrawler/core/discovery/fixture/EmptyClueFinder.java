package com.retrocrawler.core.discovery.fixture;

import java.util.Set;

import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.PathNameClueFinder;

public class EmptyClueFinder implements PathNameClueFinder {

	@Override
	public Set<Clue> find(final String pathName) {
		return Set.of();
	}
}
