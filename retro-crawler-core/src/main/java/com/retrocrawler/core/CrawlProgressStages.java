package com.retrocrawler.core;

import com.retrocrawler.core.progress.ProgressStage;

/** Progress stages belonging to a crawl operation. */
public final class CrawlProgressStages {

	public static final ProgressStage PLANNING = ProgressStage.of("PLANNING");

	public static final ProgressStage CRAWLING = ProgressStage.of("CRAWLING");

	public static final ProgressStage STOWING = ProgressStage.of("STOWING");

	public static final ProgressStage RESOLVING = ProgressStage.of("RESOLVING");

	private CrawlProgressStages() {
		// constants only
	}
}
