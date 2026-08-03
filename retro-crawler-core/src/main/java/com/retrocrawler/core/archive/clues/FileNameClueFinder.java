package com.retrocrawler.core.archive.clues;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

public interface FileNameClueFinder extends ClueFinder {

	/**
	 * Paths are guaranteed to represent files and are relative to their
	 * configured archive root. Finders should retain this relative form when a
	 * clue refers to an archive file.
	 */
	Set<Clue> find(Collection<Path> files);

	/**
	 * Serializes an archive-relative path with a stable separator for a
	 * portable clue cache.
	 */
	static String portablePath(final Path path) {
		final String separator = path.getFileSystem().getSeparator();
		return "/".equals(separator) ? path.toString() : path.toString().replace(separator, "/");
	}

}
