package com.retrocrawler.demo.gear;

import com.retrocrawler.core.annotation.RetroArchive;
import com.retrocrawler.demo.DemoFiles;
import com.retrocrawler.demo.clues.ImageClueFinder;
import com.retrocrawler.demo.clues.SquareBracketsClueFinder;

@RetroArchive(id = "retro_pc_demo", name = "Retro PC (Demo)", locations = DemoFiles.DEMO_ARCHIVE_PARENT
		+ "/retro_pc", findClues = @RetroArchive.LookAt(pathName = SquareBracketsClueFinder.class, fileNames = {
				ImageClueFinder.class }))
public class RetroPCDemoArchive {

}
