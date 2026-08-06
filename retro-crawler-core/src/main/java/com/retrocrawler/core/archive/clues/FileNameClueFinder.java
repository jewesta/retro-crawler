package com.retrocrawler.core.archive.clues;

import java.nio.file.Path;
import java.util.Collection;

public interface FileNameClueFinder extends ClueFinder {

	/**
	 * Paths are guaranteed to represent files and are relative to the current
	 * folder. Finders should retain this artifact-relative form when a clue
	 * refers to an archive file.
	 */
	Clues find(Collection<Path> files);

	/**
	 * Serializes an artifact-relative path with a stable separator for a
	 * portable clue cache.
	 */
	static String portablePath(final Path path) {
		final String separator = path.getFileSystem().getSeparator();
		return "/".equals(separator) ? path.toString() : path.toString().replace(separator, "/");
	}

}
