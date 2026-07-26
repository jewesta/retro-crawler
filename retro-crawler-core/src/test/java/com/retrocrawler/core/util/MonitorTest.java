package com.retrocrawler.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class MonitorTest {

	@Test
	void reportsStructuredProgressAndPreservesLegacyMessages() {
		final List<String> messages = new ArrayList<>();
		final List<CrawlProgress> progress = new ArrayList<>();
		final Monitor monitor = new Monitor(messages::add, progress::add);
		final CrawlProgress update = CrawlProgress.approximate(CrawlProgress.Phase.CRAWLING,
				"Region 2 of 10", 1, 10);

		monitor.report(update);

		assertEquals(List.of("Region 2 of 10"), messages);
		assertEquals(List.of(update), progress);
		assertTrue(update.isDeterminate());
		assertEquals(0.1, update.fraction().orElseThrow());
	}

	@Test
	void cancellationIsOneWayAndStopsFurtherProgress() {
		final List<CrawlProgress> progress = new ArrayList<>();
		final Monitor monitor = Monitor.observing(progress::add);

		monitor.cancel("Stopping.");
		monitor.cancel("Ignored.");
		monitor.postUpdate("Also ignored.");

		assertTrue(monitor.isCancelled());
		assertEquals(1, progress.size());
		assertEquals(CrawlProgress.Phase.CANCELLED, progress.getFirst().phase());
		assertThrows(CrawlCancelledException.class, monitor::throwIfCancelled);
	}

	@Test
	void representsIndeterminateProgressWithoutInventingAFraction() {
		final CrawlProgress progress = CrawlProgress.indeterminate(CrawlProgress.Phase.PLANNING, "Planning.");

		assertFalse(progress.isDeterminate());
		assertTrue(progress.fraction().isEmpty());
	}
}
