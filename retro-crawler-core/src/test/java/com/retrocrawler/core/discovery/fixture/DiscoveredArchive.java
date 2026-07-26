package com.retrocrawler.core.discovery.fixture;

import com.retrocrawler.core.annotation.RetroArchive;

@RetroArchive(id = "discovered_model", locations = "/this/path/is/not-read-during-model-creation",
		findClues = @RetroArchive.LookAt(pathName = EmptyClueFinder.class))
class DiscoveredArchive {
	// Annotation marker type.
}
