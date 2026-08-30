package com.retrocrawler.core.stash;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * When one folder's most recent crawl operation started and when that folder
 * had been fully observed.
 */
public record CrawlObservation(Instant crawlStartedAt, Instant observedAt) {

	public CrawlObservation {
		Objects.requireNonNull(crawlStartedAt, "crawlStartedAt");
		Objects.requireNonNull(observedAt, "observedAt");
	}

	/**
	 * Time from the start of the operation until this observation completed.
	 */
	public Duration elapsed() {
		return Duration.between(crawlStartedAt, observedAt);
	}

}
