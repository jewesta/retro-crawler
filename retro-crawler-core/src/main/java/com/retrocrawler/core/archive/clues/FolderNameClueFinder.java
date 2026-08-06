package com.retrocrawler.core.archive.clues;

/**
 * Looks at the name of the current collection folder.
 */
public interface FolderNameClueFinder extends ClueFinder {

	Clues find(String folderName);
}
