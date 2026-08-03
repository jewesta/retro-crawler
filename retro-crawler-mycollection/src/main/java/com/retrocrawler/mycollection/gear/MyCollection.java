package com.retrocrawler.mycollection.gear;

import com.retrocrawler.core.annotation.RetroClues;
import com.retrocrawler.core.annotation.RetroCollection;
import com.retrocrawler.mycollection.clues.BracketClueFinder;
import com.retrocrawler.mycollection.clues.FloppyImageClueFinder;
import com.retrocrawler.mycollection.clues.RetroMarkdownClueFinder;
import com.retrocrawler.mycollection.clues.StandardImageClueFinder;

@RetroCollection(id = "my_collection", name = "My Collection")
@RetroClues(fromFolderName = BracketClueFinder.class,
		fromFileContents = RetroMarkdownClueFinder.class,
		fromFileNames = { StandardImageClueFinder.class, FloppyImageClueFinder.class })
public final class MyCollection {

	private MyCollection() {
		// Model declaration.
	}
}
