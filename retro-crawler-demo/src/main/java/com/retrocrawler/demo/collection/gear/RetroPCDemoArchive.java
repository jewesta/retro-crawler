package com.retrocrawler.demo.collection.gear;

import com.retrocrawler.core.annotation.RetroArchive;
import com.retrocrawler.demo.collection.clues.ImageClueFinder;
import com.retrocrawler.demo.collection.clues.SquareBracketsClueFinder;

@RetroArchive(id = "retro_pc_demo", name = "Retro PC (Demo)", locations = "archives/retro_pc", findClues = @RetroArchive.LookAt(pathName = SquareBracketsClueFinder.class, fileNames = {
		ImageClueFinder.class }))
public class RetroPCDemoArchive {

}
