package com.retrocrawler.mycollection.gear;

import com.retrocrawler.core.annotation.RetroArchive;
import com.retrocrawler.mycollection.clues.BracketPathClueFinder;
import com.retrocrawler.mycollection.clues.FloppyImageClueFinder;
import com.retrocrawler.mycollection.clues.RetroMarkdownClueFinder;
import com.retrocrawler.mycollection.clues.RetroPropertiesClueFinder;
import com.retrocrawler.mycollection.clues.StandardImageClueFinder;
import com.retrocrawler.mycollection.clues.WebReferenceClueFinder;

@RetroArchive(id = "my_collection", name = "My Collection",
		findClues = @RetroArchive.LookAt(pathName = BracketPathClueFinder.class,
				fileContents = { RetroPropertiesClueFinder.class, RetroMarkdownClueFinder.class },
				fileNames = { StandardImageClueFinder.class, FloppyImageClueFinder.class,
						WebReferenceClueFinder.class }))
public final class MyCollectionArchive {

	private MyCollectionArchive() {
		// Model declaration.
	}
}
