package com.retrocrawler.core.archive;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Decides which direct filesystem entries belong to an archive crawl.
 * <p>
 * Policies are consulted before an entry is classified as a file or folder. A
 * rejected folder is therefore pruned with its complete subtree. Annotation-
 * configured policies must provide a public no-argument constructor.
 */
@FunctionalInterface
public interface CrawlPolicy {

	/**
	 * Returns whether the crawler should admit the given direct archive entry.
	 */
	boolean includes(Path entry);

	/**
	 * Combines policies so that an entry must be admitted by both.
	 */
	default CrawlPolicy and(final CrawlPolicy other) {
		Objects.requireNonNull(other, "other");
		return entry -> includes(entry) && other.includes(entry);
	}
}
