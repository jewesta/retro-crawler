package com.retrocrawler.core.archive;

import java.time.Duration;
import java.util.Objects;

/**
 * Bounds the shallow analysis sweep used to divide an archive into approximate
 * progress regions.
 */
public record CrawlPlanning(int targetRegions, int maximumDepth, int maximumAnalyzedDirectories,
		Duration maximumDuration) {

	private static final int DEFAULT_TARGET_REGIONS = 100;

	private static final int DEFAULT_MAXIMUM_DEPTH = 6;

	private static final int DEFAULT_MAXIMUM_ANALYZED_DIRECTORIES = 2_000;

	private static final Duration DEFAULT_MAXIMUM_DURATION = Duration.ofSeconds(5);

	public CrawlPlanning {
		if (targetRegions < 1) {
			throw new IllegalArgumentException("targetRegions must be at least one.");
		}
		if (maximumDepth < 0) {
			throw new IllegalArgumentException("maximumDepth must not be negative.");
		}
		if (maximumAnalyzedDirectories < 1) {
			throw new IllegalArgumentException("maximumAnalyzedDirectories must be at least one.");
		}
		Objects.requireNonNull(maximumDuration, "maximumDuration");
		if (maximumDuration.isNegative() || maximumDuration.isZero()) {
			throw new IllegalArgumentException("maximumDuration must be positive.");
		}
	}

	public static CrawlPlanning defaults() {
		return new CrawlPlanning(DEFAULT_TARGET_REGIONS, DEFAULT_MAXIMUM_DEPTH,
				DEFAULT_MAXIMUM_ANALYZED_DIRECTORIES, DEFAULT_MAXIMUM_DURATION);
	}
}
