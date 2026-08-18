package com.retrocrawler.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.progress.ProgressCancelledException;
import com.retrocrawler.core.progress.ProgressState;
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
	void exposesTheConfiguredProgressorAsReadOnlyProgress() {
		final Progressor progressor = Progressor.create();

		assertSame(progressor, new Journal(progressor).progress());
	}

	@Test
	void tracksSuccessfulOperationToCompletion() throws Exception {
		final Progressor progressor = Progressor.create();
		final Journal journal = new Journal(progressor);

		final String result = journal.track("Refresh", () -> "finished");

		assertEquals("finished", result);
		assertEquals(ProgressState.COMPLETE, progressor.state());
		assertEquals("Refresh complete.", progressor.message());
	}

	@Test
	void tracksFailedOperationAndPropagatesTheSameException() {
		final Progressor progressor = Progressor.create();
		final Journal journal = new Journal(progressor);
		final Exception expected = new Exception("Broken.");

		final Exception thrown = assertThrows(Exception.class, () -> journal.track("Refresh", () -> {
			throw expected;
		}));

		assertSame(expected, thrown);
		assertEquals(ProgressState.FAILED, progressor.state());
		assertEquals("Refresh failed: Broken.", progressor.message());
	}

	@Test
	void preservesCancellationAsItsOwnTerminalState() {
		final Progressor progressor = Progressor.create();
		final Journal journal = new Journal(progressor);

		assertThrows(ProgressCancelledException.class, () -> journal.track("Refresh", () -> {
			journal.cancel("Stopping.");
			journal.throwIfCancelled();
			return null;
		}));

		assertEquals(ProgressState.CANCELLED, progressor.state());
		assertEquals("Stopping.", progressor.message());
	}

	@Test
	void finishedOperationCannotBeCancelledOrTrackedAgain() throws Exception {
		final Progressor progressor = Progressor.create();
		final Journal journal = new Journal(progressor);
		journal.track("Refresh", () -> null);

		journal.cancel("Too late.");

		assertEquals(ProgressState.COMPLETE, progressor.state());
		assertEquals("Refresh complete.", progressor.message());
		assertThrows(IllegalStateException.class, () -> journal.track("Another refresh", () -> null));
	}

}
