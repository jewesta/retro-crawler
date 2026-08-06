package com.retrocrawler.core.archive.clues;

import java.io.InputStream;

public interface FileContentClueFinder extends ClueFinder {

	/**
	 * A {@link FileContentClueFinder} can never match more than one file and
	 * two or more {@link FileContentClueFinder}s can never match the same file.
	 */
	boolean matches(String fileName);

	Clues find(InputStream is);

}
