package com.retrocrawler.app.collection.gear;

import com.retrocrawler.app.collection.clues.ImageClueFinder;
import com.retrocrawler.app.collection.clues.SquareBracketsClueFinder;
import com.retrocrawler.core.annotation.RetroArchive;

@RetroArchive(id = "retro_pc_demo", name = "Retro PC (Demo)", locations = "archives/retro_pc", findClues = @RetroArchive.LookAt(pathName = SquareBracketsClueFinder.class, fileNames = {
		ImageClueFinder.class }))
public class Socket7Archive {

}
