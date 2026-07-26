package com.retrocrawler.mycollection.gear;

import com.retrocrawler.core.annotation.RetroArchive;
import com.retrocrawler.mycollection.clues.BracketPathClueFinder;

@RetroArchive(id = "my_collection", name = "My Collection",
		findClues = @RetroArchive.LookAt(pathName = BracketPathClueFinder.class))
public final class MyCollectionArchive {

	private MyCollectionArchive() {
		// Model declaration.
	}
}
