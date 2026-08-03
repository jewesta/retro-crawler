package com.retrocrawler.core.archive.filter;

import java.nio.file.Path;

/**
 * Decides which direct filesystem entries belong to an archive.
 * <p>
 * Filters are consulted before an entry is classified as a file or folder. A
 * rejected folder is therefore pruned with its complete subtree. Annotation-
 * configured filters must provide a public no-argument constructor.
 */
@FunctionalInterface
public interface ArchivePathFilter {

	/**
	 * Returns whether the crawler should accept the given direct archive entry.
	 */
	boolean accept(Path path);
}
