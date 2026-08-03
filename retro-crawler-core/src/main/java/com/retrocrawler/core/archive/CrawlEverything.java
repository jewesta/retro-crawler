package com.retrocrawler.core.archive;

import java.nio.file.Path;

/**
 * Preserves every regular file and folder encountered by the crawler.
 */
public final class CrawlEverything implements CrawlPolicy {

	@Override
	public boolean includes(final Path entry) {
		return true;
	}
}
