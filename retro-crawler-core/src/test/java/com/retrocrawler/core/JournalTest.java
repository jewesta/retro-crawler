package com.retrocrawler.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.progress.Progressor;

class JournalTest {

	@Test
	void recordsAndRethrowsTheSameExceptionByDefault() {
		final Journal journal = new Journal();
		final Exception expected = new Exception("Broken.");

		final Exception thrown = assertThrows(Exception.class, () -> journal.record(expected));

		assertSame(expected, thrown);
		assertEquals(FailureMode.FAIL_EARLY, journal.failureMode());
		assertEquals(List.of(expected), journal.failures());
	}

	@Test
	void failLateRecordsEveryExceptionInEncounterOrder() throws Exception {
		final Journal journal = new Journal(FailureMode.FAIL_LATE);
		final Exception first = new IllegalArgumentException("First.");
		final Exception second = new Exception("Second.");

		journal.record(first);
		journal.record(second);

		assertEquals(List.of(first, second), journal.failures());
		assertEquals(2, journal.failureCount());
		assertTrue(journal.hasFailures());
	}

	@Test
	void retainsTheConfiguredProgressor() {
		final Progressor progressor = Progressor.create();

		assertSame(progressor, new Journal(progressor).progressor());
	}

}
