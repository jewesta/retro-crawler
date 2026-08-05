package com.retrocrawler.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class CrawlExceptionTest {

	@Test
	void retainsEveryExceptionButBoundsTheRenderedReport() {
		final List<Exception> failures = new ArrayList<>();
		for (int index = 1; index <= 50; index++) {
			failures.add(new IllegalArgumentException("Visible " + index));
		}
		failures.add(new IllegalStateException("Hidden alpha"));
		failures.add(new UnsupportedOperationException("Hidden beta"));

		final CrawlException report = new CrawlException(failures);

		assertEquals(failures, report.failures());
		assertTrue(report.getMessage().contains("52 recorded exceptions; showing 50"));
		assertTrue(report.getMessage().contains("50. IllegalArgumentException: Visible 50"));
		assertTrue(report.getMessage().contains("2 further exceptions are not shown."));
		assertFalse(report.getMessage().contains("Hidden alpha"));
		assertFalse(report.getMessage().contains("Hidden beta"));
	}

	@Test
	void requiresAtLeastOneRecordedException() {
		assertThrows(IllegalArgumentException.class, () -> new CrawlException(List.of()));
	}
}
