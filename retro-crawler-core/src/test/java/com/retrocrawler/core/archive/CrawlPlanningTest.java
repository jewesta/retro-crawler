package com.retrocrawler.core.archive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class CrawlPlanningTest {

	@Test
	void suppliesBoundedDefaults() {
		final CrawlPlanning planning = CrawlPlanning.defaults();

		assertEquals(100, planning.targetRegions());
		assertEquals(6, planning.maximumDepth());
		assertEquals(2_000, planning.maximumAnalyzedDirectories());
		assertEquals(Duration.ofSeconds(5), planning.maximumDuration());
	}

	@Test
	void rejectsUnboundedOrMeaninglessValues() {
		assertThrows(IllegalArgumentException.class,
				() -> new CrawlPlanning(0, 1, 1, Duration.ofSeconds(1)));
		assertThrows(IllegalArgumentException.class,
				() -> new CrawlPlanning(1, -1, 1, Duration.ofSeconds(1)));
		assertThrows(IllegalArgumentException.class,
				() -> new CrawlPlanning(1, 1, 0, Duration.ofSeconds(1)));
		assertThrows(IllegalArgumentException.class,
				() -> new CrawlPlanning(1, 1, 1, Duration.ZERO));
	}
}
