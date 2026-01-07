package com.retrocrawler.app.collection.gear;

import com.retrocrawler.app.collection.clues.ImageClueFinder;
import com.retrocrawler.app.collection.clues.SquareBracketsClueFinder;
import com.retrocrawler.core.annotation.RetroArchive;

@RetroArchive(id = "socket7", name = "Socket 7 Motherboards", locations = "/Volumes/Daten/Systeme/IBM und Kompatible/Komponenten/Hauptplatinen/Socket 7", findClues = @RetroArchive.LookAt(pathName = SquareBracketsClueFinder.class, fileNames = {
		ImageClueFinder.class }))
public class Socket7Archive {

}
