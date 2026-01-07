package com.retrocrawler.core.archive.clues;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

public interface FileNameClueFinder extends ClueFinder {

	/**
	 * Paths are guaranteed to be files
	 */
	Set<Clue> find(Collection<Path> files);

}
