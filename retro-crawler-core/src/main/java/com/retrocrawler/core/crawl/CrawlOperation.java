package com.retrocrawler.core.crawl;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.retrocrawler.core.archive.ReindexScope;
import com.retrocrawler.core.progress.ProgressSnapshot;

/** Immutable status snapshot of one asynchronous crawl operation. */
public record CrawlOperation(CrawlOperationId id, ReindexScope scope, CrawlOperationState state, Instant startedAt,
		Optional<Instant> finishedAt, ProgressSnapshot progress, List<CrawlOperationFailure> failures) {

	public CrawlOperation {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(scope, "scope");
		Objects.requireNonNull(state, "state");
		Objects.requireNonNull(startedAt, "startedAt");
		finishedAt = Objects.requireNonNull(finishedAt, "finishedAt");
		Objects.requireNonNull(progress, "progress");
		failures = List.copyOf(Objects.requireNonNull(failures, "failures"));

		if (isActive(state) && finishedAt.isPresent()) {
			throw new IllegalArgumentException("An active crawl operation must not have a finish time.");
		}
		if (!isActive(state) && finishedAt.isEmpty()) {
			throw new IllegalArgumentException("A finished crawl operation requires a finish time.");
		}
	}

	public boolean isActive() {
		return isActive(state);
	}

	private static boolean isActive(final CrawlOperationState state) {
		return state == CrawlOperationState.RUNNING || state == CrawlOperationState.CANCELLING;
	}
}
