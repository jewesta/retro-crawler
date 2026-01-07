package com.retrocrawler.core.archive.clues;

import java.util.Set;

/**
 * Looks at the path (name of the current folder) of the current archive node.
 */
public interface PathNameClueFinder extends ClueFinder {

	Set<Clue> find(String pathName);

}
