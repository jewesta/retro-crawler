package com.retrocrawler.core.crawl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.Journal;
import com.retrocrawler.core.RetroCrawler;
import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.archive.ReindexScope;
import com.retrocrawler.core.archive.source.ArchiveFileAccessor;
import com.retrocrawler.core.gear.filter.FilterDefinition;
import com.retrocrawler.core.progress.ProgressState;
import com.retrocrawler.core.stash.Stash;

class CrawlOperationServiceTest {

	private static final Duration TIMEOUT = Duration.ofSeconds(5);

	private static final Stash EMPTY_STASH = new Stash(List.of());

	@Test
	void runsOneCrawlAndRetainsItsSuccessfulStatus() throws Exception {
		final CountDownLatch entered = new CountDownLatch(1);
		final CountDownLatch release = new CountDownLatch(1);
		final AtomicReference<ReindexScope> observedScope = new AtomicReference<>();
		final RetroCrawler crawler = crawler((journal, scope) -> journal.track("Crawl", () -> {
			observedScope.set(scope);
			entered.countDown();
			await(release);
			return EMPTY_STASH;
		}));
		final CrawlOperationService service = new CrawlOperationService(crawler);

		try {
			final ReindexScope scope = ReindexScope.all();
			final CrawlOperation started = service.start(scope);
			assertTrue(entered.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));

			assertEquals(CrawlOperationState.RUNNING, started.state());
			assertSame(scope, started.scope());
			assertEquals(started.id(), service.active().orElseThrow().id());
			final CrawlAlreadyRunningException conflict = assertThrows(CrawlAlreadyRunningException.class,
					() -> service.start(ReindexScope.all()));
			assertEquals(started.id(), conflict.activeOperationId());

			release.countDown();
			final CrawlOperation finished = awaitFinished(service, started.id());
			assertSame(scope, observedScope.get());
			assertEquals(CrawlOperationState.SUCCEEDED, finished.state());
			assertEquals(ProgressState.COMPLETE, finished.progress().state());
			assertTrue(finished.finishedAt().isPresent());
			assertTrue(finished.failures().isEmpty());
			assertTrue(service.active().isEmpty());
		} finally {
			release.countDown();
			service.close();
		}
	}

	@Test
	void cooperativelyCancelsAnActiveCrawlBeforeReleasingItsSlot() throws Exception {
		final CountDownLatch entered = new CountDownLatch(1);
		final CountDownLatch release = new CountDownLatch(1);
		final RetroCrawler crawler = crawler((journal, scope) -> journal.track("Crawl", () -> {
			entered.countDown();
			await(release);
			journal.throwIfCancelled();
			return EMPTY_STASH;
		}));
		final CrawlOperationService service = new CrawlOperationService(crawler);

		try {
			final CrawlOperation started = service.start(ReindexScope.all());
			assertTrue(entered.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));

			final CrawlOperation cancelling = service.cancel(started.id());
			assertEquals(CrawlOperationState.CANCELLING, cancelling.state());
			assertEquals(ProgressState.CANCELLED, cancelling.progress().state());
			assertThrows(CrawlAlreadyRunningException.class, () -> service.start(ReindexScope.all()));

			release.countDown();
			final CrawlOperation cancelled = awaitFinished(service, started.id());
			assertEquals(CrawlOperationState.CANCELLED, cancelled.state());
			assertEquals(ProgressState.CANCELLED, cancelled.progress().state());
			assertTrue(cancelled.finishedAt().isPresent());
			assertTrue(cancelled.failures().isEmpty());
			assertFalse(cancelled.isActive());
		} finally {
			release.countDown();
			service.close();
		}
	}

	@Test
	void retainsAStableFailureDescription() throws Exception {
		final RetroCrawler crawler = crawler((journal, scope) -> journal.track("Crawl", () -> {
			throw new IOException("Broken archive.");
		}));

		try (CrawlOperationService service = new CrawlOperationService(crawler)) {
			final CrawlOperation started = service.start(ReindexScope.all());
			final CrawlOperation failed = awaitFinished(service, started.id());

			assertEquals(CrawlOperationState.FAILED, failed.state());
			assertEquals(ProgressState.FAILED, failed.progress().state());
			assertEquals(List.of(new CrawlOperationFailure(IOException.class.getName(), "Broken archive.")),
					failed.failures());
		}
	}

	@Test
	void rejectsNonPhysicalScopesAndUnknownOperations() {
		try (CrawlOperationService service = new CrawlOperationService(crawler((journal, scope) -> EMPTY_STASH))) {
			final IllegalArgumentException scopeFailure = assertThrows(IllegalArgumentException.class,
					() -> service.start(ReindexScope.none()));
			final CrawlOperationId unknown = new CrawlOperationId("unknown");
			final IllegalArgumentException operationFailure = assertThrows(IllegalArgumentException.class,
					() -> service.cancel(unknown));

			assertEquals(
					"A crawl operation requires a physical reindex scope; use RetroCrawler.access to reuse stored clues.",
					scopeFailure.getMessage());
			assertEquals("Unknown crawl operation: unknown", operationFailure.getMessage());
			assertTrue(service.operation(unknown).isEmpty());
		}
	}

	@Test
	void boundsRetainedOperationHistory() throws Exception {
		final Clock clock = Clock.fixed(Instant.parse("2026-08-30T12:00:00Z"), ZoneOffset.UTC);
		try (CrawlOperationService service = new CrawlOperationService(
				crawler((journal, scope) -> journal.track("Crawl", () -> EMPTY_STASH)),
				Executors.newSingleThreadExecutor(), clock, 1)) {
			final CrawlOperation first = service.start(ReindexScope.all());
			awaitFinished(service, first.id());
			final CrawlOperation second = service.start(ReindexScope.all());
			awaitFinished(service, second.id());

			assertTrue(service.operation(first.id()).isEmpty());
			assertEquals(second.id(), service.operation(second.id()).orElseThrow().id());
		}
	}

	private static CrawlOperation awaitFinished(final CrawlOperationService service, final CrawlOperationId id)
			throws InterruptedException {
		final long deadline = System.nanoTime() + TIMEOUT.toNanos();
		while (System.nanoTime() < deadline) {
			final CrawlOperation operation = service.operation(id).orElseThrow();
			if (!operation.isActive()) {
				return operation;
			}
			Thread.sleep(5);
		}
		return fail("Crawl operation did not finish within " + TIMEOUT + ".");
	}

	private static void await(final CountDownLatch latch) throws IOException {
		try {
			if (!latch.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
				throw new IOException("Timed out waiting for test crawl release.");
			}
		} catch (final InterruptedException interruption) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while waiting for test crawl release.", interruption);
		}
	}

	private static RetroCrawler crawler(final CrawlAction crawl) {
		return new RetroCrawler() {

			@Override
			public List<ArchiveDescriptor> archives() {
				return List.of();
			}

			@Override
			public List<FilterDefinition<?>> filters() {
				return List.of();
			}

			@Override
			public String collectionId() {
				return "operation_test";
			}

			@Override
			public ArchiveDescriptor archive(final ArchiveId archiveId) {
				throw new UnsupportedOperationException();
			}

			@Override
			public <T> Optional<T> inspect(final ARI source, final ArchiveFileAccessor<T> inspector) {
				throw new UnsupportedOperationException();
			}

			@Override
			public Stash access(final Journal journal) {
				throw new UnsupportedOperationException();
			}

			@Override
			public Stash crawl(final Journal journal, final ReindexScope reindexScope) throws IOException {
				return crawl.perform(journal, reindexScope);
			}
		};
	}

	@FunctionalInterface
	private interface CrawlAction {

		Stash perform(Journal journal, ReindexScope scope) throws IOException;
	}
}
