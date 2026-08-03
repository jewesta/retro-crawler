package com.retrocrawler.demo.gear;

import com.retrocrawler.core.annotation.RetroClues;
import com.retrocrawler.core.annotation.RetroCollection;
import com.retrocrawler.demo.DemoFiles;
import com.retrocrawler.demo.clues.ImageClueFinder;
import com.retrocrawler.demo.clues.SquareBracketsClueFinder;

@RetroCollection(id = "retro_pc_demo", name = "Retro PC (Demo)",
		locations = DemoFiles.DEMO_ARCHIVE_PARENT + "/retro_pc")
@RetroClues(fromFolderName = SquareBracketsClueFinder.class,
		fromFileNames = ImageClueFinder.class)
public final class RetroPCDemoCollection {

	private RetroPCDemoCollection() {
		// Model declaration.
	}
}
