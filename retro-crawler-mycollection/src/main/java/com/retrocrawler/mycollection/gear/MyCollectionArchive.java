package com.retrocrawler.mycollection.gear;

import com.retrocrawler.core.annotation.RetroArchive;
import com.retrocrawler.mycollection.clues.BracketPathClueFinder;
import com.retrocrawler.mycollection.clues.FloppyImageClueFinder;
import com.retrocrawler.mycollection.clues.RetroMarkdownClueFinder;
import com.retrocrawler.mycollection.clues.StandardImageClueFinder;

@RetroArchive(id = "my_collection", name = "My Collection",
		findClues = @RetroArchive.LookAt(pathName = BracketPathClueFinder.class,
				fileContents = RetroMarkdownClueFinder.class,
				fileNames = { StandardImageClueFinder.class, FloppyImageClueFinder.class }))
public final class MyCollectionArchive {

	private MyCollectionArchive() {
		// Model declaration.
	}
}
