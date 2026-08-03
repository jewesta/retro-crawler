package com.retrocrawler.core.discovery.fixture;

import com.retrocrawler.core.annotation.RetroClues;
import com.retrocrawler.core.annotation.RetroCollection;

@RetroCollection(id = "discovered_model", locations = "/this/path/is/not-read-during-model-creation")
@RetroClues(fromFolderName = EmptyClueFinder.class)
class DiscoveredArchive {
	// Annotation marker type.
}
