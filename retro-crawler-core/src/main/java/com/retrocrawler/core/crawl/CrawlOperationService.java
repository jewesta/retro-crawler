package com.retrocrawler.core.crawl;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.retrocrawler.core.CrawlException;
import com.retrocrawler.core.Journal;
import com.retrocrawler.core.RetroCrawler;
import com.retrocrawler.core.archive.ReindexScope;
import com.retrocrawler.core.progress.ProgressCancelledException;

/**
 * Runs and observes asynchronous crawls for one {@link RetroCrawler}.
 * <p>
 * The service admits only one crawl at a time. It owns operation progress and
 * cancellation, while the crawler remains the sole owner of its current
 * immutable Stash and its atomic publication.
 */
public final class CrawlOperationService implements AutoCloseable {

	private static final int DEFAULT_RETAINED_OPERATIONS = 32;

	private static final String CANCELLATION_MESSAGE = "Cancellation requested.";

	private static final String SHUTDOWN_MESSAGE = "Crawl service is shutting down.";

	private final RetroCrawler crawler;

	private final ExecutorService executor;

	private final Clock clock;

	private final int retainedOperations;

	private final Object lock = new Object();

	private final Map<CrawlOperationId, MutableOperation> operations = new LinkedHashMap<>();

	private MutableOperation active;

	private boolean closed;

	public CrawlOperationService(final RetroCrawler crawler) {
		this(crawler, Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("retro-crawler-crawl-", 0).factory()),
				Clock.systemUTC(), DEFAULT_RETAINED_OPERATIONS);
	}

	CrawlOperationService(final RetroCrawler crawler, final ExecutorService executor, final Clock clock,
			final int retainedOperations) {
		this.crawler = Objects.requireNonNull(crawler, "crawler");
		this.executor = Objects.requireNonNull(executor, "executor");
		this.clock = Objects.requireNonNull(clock, "clock");
		if (retainedOperations <= 0) {
			throw new IllegalArgumentException("At least one crawl operation must be retained.");
		}
		this.retainedOperations = retainedOperations;
	}

	/** Starts a physical crawl and immediately returns its operation status. */
	public CrawlOperation start(final ReindexScope scope) {
		final ReindexScope requestedScope = Objects.requireNonNull(scope, "scope");
		if (requestedScope.kind() == ReindexScope.Kind.NONE) {
			throw new IllegalArgumentException(
					"A crawl operation requires a physical reindex scope; use RetroCrawler.access to reuse stored clues.");
		}

		final MutableOperation operation;
		synchronized (lock) {
			if (closed) {
				throw new IllegalStateException("Crawl operation service is closed.");
			}
			if (active != null) {
				throw new CrawlAlreadyRunningException(active.id());
			}

			operation = new MutableOperation(CrawlOperationId.create(), requestedScope, clock.instant());
			retain(operation);
			active = operation;
			try {
				executor.execute(() -> run(operation));
			} catch (final RuntimeException failure) {
				active = null;
				operations.remove(operation.id());
				throw failure;
			}
		}
		return operation.snapshot();
	}

	/** Returns one retained operation, if its ID is known. */
	public Optional<CrawlOperation> operation(final CrawlOperationId id) {
		Objects.requireNonNull(id, "id");
		synchronized (lock) {
			return Optional.ofNullable(operations.get(id)).map(MutableOperation::snapshot);
		}
	}

	/** Returns the operation still occupying the single crawl slot. */
	public Optional<CrawlOperation> active() {
		synchronized (lock) {
			return Optional.ofNullable(active).map(MutableOperation::snapshot);
		}
	}

	/** Requests cooperative cancellation and returns the resulting status. */
	public CrawlOperation cancel(final CrawlOperationId id) {
		Objects.requireNonNull(id, "id");
		final MutableOperation operation;
		synchronized (lock) {
			operation = operations.get(id);
		}
		if (operation == null) {
			throw new IllegalArgumentException("Unknown crawl operation: " + id);
		}
		operation.cancel(CANCELLATION_MESSAGE);
		return operation.snapshot();
	}

	@Override
	public void close() {
		final MutableOperation running;
		synchronized (lock) {
			if (closed) {
				return;
			}
			closed = true;
			running = active;
		}
		if (running != null) {
			running.cancel(SHUTDOWN_MESSAGE);
		}
		executor.close();
	}

	private void run(final MutableOperation operation) {
		try {
			crawler.crawl(operation.journal(), operation.scope());
			finish(operation, null);
		} catch (final ProgressCancelledException cancellation) {
			cancelled(operation);
		} catch (final Throwable failure) {
			finish(operation, failure);
			if (failure instanceof final Error error) {
				throw error;
			}
		}
	}

	private void finish(final MutableOperation operation, final Throwable failure) {
		synchronized (lock) {
			if (failure == null) {
				operation.succeed(clock.instant());
			} else {
				operation.fail(failure, clock.instant());
			}
			release(operation);
		}
	}

	private void cancelled(final MutableOperation operation) {
		synchronized (lock) {
			operation.cancelled(clock.instant());
			release(operation);
		}
	}

	private void release(final MutableOperation operation) {
		if (active == operation) {
			active = null;
		}
	}

	private void retain(final MutableOperation operation) {
		while (operations.size() >= retainedOperations) {
			final CrawlOperationId oldest = operations.keySet().iterator().next();
			operations.remove(oldest);
		}
		operations.put(operation.id(), operation);
	}

	private static final class MutableOperation {

		private final CrawlOperationId id;

		private final ReindexScope scope;

		private final Instant startedAt;

		private final Journal journal = new Journal();

		private CrawlOperationState state = CrawlOperationState.RUNNING;

		private Instant finishedAt;

		private Throwable terminalFailure;

		private MutableOperation(final CrawlOperationId id, final ReindexScope scope, final Instant startedAt) {
			this.id = id;
			this.scope = scope;
			this.startedAt = startedAt;
		}

		private CrawlOperationId id() {
			return id;
		}

		private ReindexScope scope() {
			return scope;
		}

		private Journal journal() {
			return journal;
		}

		private synchronized void cancel(final String message) {
			if (state != CrawlOperationState.RUNNING) {
				return;
			}
			journal.cancel(message);
			if (journal.isCancelled()) {
				state = CrawlOperationState.CANCELLING;
			}
		}

		private synchronized void succeed(final Instant finished) {
			finishedAt = Objects.requireNonNull(finished, "finished");
			state = state == CrawlOperationState.CANCELLING ? CrawlOperationState.CANCELLED
					: CrawlOperationState.SUCCEEDED;
		}

		private synchronized void cancelled(final Instant finished) {
			finishedAt = Objects.requireNonNull(finished, "finished");
			state = CrawlOperationState.CANCELLED;
		}

		private synchronized void fail(final Throwable failure, final Instant finished) {
			terminalFailure = Objects.requireNonNull(failure, "failure");
			finishedAt = Objects.requireNonNull(finished, "finished");
			state = state == CrawlOperationState.CANCELLING ? CrawlOperationState.CANCELLED
					: CrawlOperationState.FAILED;
		}

		private synchronized CrawlOperation snapshot() {
			return new CrawlOperation(id, scope, state, startedAt, Optional.ofNullable(finishedAt),
					journal.progress().snapshot(), failures());
		}

		private List<CrawlOperationFailure> failures() {
			final List<Exception> recorded = journal.failures();
			final List<CrawlOperationFailure> result = new ArrayList<>(
					recorded.stream().map(CrawlOperationFailure::from).toList());
			if (terminalFailure != null && !containsIdentity(recorded, terminalFailure)
					&& !(terminalFailure instanceof CrawlException && !recorded.isEmpty())) {
				result.add(CrawlOperationFailure.from(terminalFailure));
			}
			return List.copyOf(result);
		}

		private static boolean containsIdentity(final List<Exception> failures, final Throwable expected) {
			for (final Exception failure : failures) {
				if (failure == expected) {
					return true;
				}
			}
			return false;
		}
	}
}
