package com.retrocrawler.core.archive.clues;

import java.util.Set;

/**
 * Looks at the name of the current collection folder.
 */
public interface FolderNameClueFinder extends ClueFinder {

	Set<Clue> find(String folderName);
}
