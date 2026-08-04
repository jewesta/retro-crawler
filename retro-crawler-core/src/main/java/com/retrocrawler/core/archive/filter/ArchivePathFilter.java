package com.retrocrawler.core.archive.filter;

import java.nio.file.Path;

/**
 * Decides which direct source entries belong to an archive.
 * <p>
 * A path is an address interpreted by the configured archive source and is not
 * necessarily accessible through the local filesystem. A rejected folder is
 * pruned with its complete subtree. Annotation-configured filters must provide
 * a public no-argument constructor.
 */
@FunctionalInterface
public interface ArchivePathFilter {

	/**
	 * Returns whether the crawler should accept the given direct archive entry
	 * path.
	 */
	boolean accept(Path path);
}
