package com.retrocrawler.mycollection.gear;

import com.retrocrawler.core.annotation.RetroClues;
import com.retrocrawler.core.annotation.RetroCollection;
import com.retrocrawler.core.archive.filter.IgnoreDotPaths;
import com.retrocrawler.core.archive.filter.IgnoreLinuxSystemPaths;
import com.retrocrawler.core.archive.filter.IgnoreMacSystemPaths;
import com.retrocrawler.core.archive.filter.IgnoreQNAPSystemPaths;
import com.retrocrawler.core.archive.filter.IgnoreSynologySystemPaths;
import com.retrocrawler.core.archive.filter.IgnoreWindowsSystemPaths;
import com.retrocrawler.mycollection.clues.BracketClueFinder;
import com.retrocrawler.mycollection.clues.FloppyImageClueFinder;
import com.retrocrawler.mycollection.clues.RetroMarkdownClueFinder;
import com.retrocrawler.mycollection.clues.StandardImageClueFinder;

@RetroCollection(id = "my_collection", name = "My Collection", pathFilters = {
		IgnoreDotPaths.class,
		IgnoreWindowsSystemPaths.class,
		IgnoreMacSystemPaths.class,
		IgnoreLinuxSystemPaths.class,
		IgnoreQNAPSystemPaths.class,
		IgnoreSynologySystemPaths.class
})
@RetroClues(fromFolderName = BracketClueFinder.class, fromFileContents = RetroMarkdownClueFinder.class,
		fromFileNames = {
				StandardImageClueFinder.class, FloppyImageClueFinder.class
		})
public final class MyCollection {

	private MyCollection() {
		// Model declaration.
	}
}
