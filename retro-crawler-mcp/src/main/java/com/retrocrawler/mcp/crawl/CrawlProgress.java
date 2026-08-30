package com.retrocrawler.mcp.crawl;

import java.util.Objects;

/** A transport-safe snapshot of current crawl progress. */
public record CrawlProgress(String stage, String message, long completed, long total, String accuracy,
		String progressState, double progress, double overallFraction, long elapsedMillis, long remainingMillis) {

	public CrawlProgress {
		Objects.requireNonNull(stage, "stage");
		Objects.requireNonNull(message, "message");
		Objects.requireNonNull(accuracy, "accuracy");
		Objects.requireNonNull(progressState, "progressState");
	}
}
