package com.retrocrawler.mcp.crawl;

import java.time.Duration;
import java.util.List;

import com.retrocrawler.core.crawl.CrawlOperation;
import com.retrocrawler.core.crawl.CrawlOperationFailure;
import com.retrocrawler.core.progress.ProgressSnapshot;

final class CrawlStatusMapper {

	private static final int MAXIMUM_FAILURES = 100;

	private static final int MAXIMUM_TEXT_LENGTH = 2_000;

	private CrawlStatusMapper() {
	}

	static CrawlStatus from(final CrawlOperation operation) {
		final List<CrawlOperationFailure> availableFailures = operation.failures();
		final int failureCount = Math.min(availableFailures.size(), MAXIMUM_FAILURES);
		final List<CrawlFailure> failures = availableFailures.subList(0, failureCount).stream()
				.map(CrawlStatusMapper::from).toList();
		return new CrawlStatus(operation.id().value(),
				new CrawlScope(operation.scope().kind().name(),
						operation.scope().subtrees().stream().map(Object::toString).toList()),
				operation.state().name(), operation.isActive(), operation.startedAt().toString(),
				operation.finishedAt().map(Object::toString).orElse(""), from(operation.progress()), failures,
				availableFailures.size() > MAXIMUM_FAILURES);
	}

	private static CrawlProgress from(final ProgressSnapshot progress) {
		return new CrawlProgress(progress.stage().value(), abbreviate(progress.message()), progress.completed(),
				progress.total(), progress.accuracy().name(), progress.state().name(), progress.progress(),
				progress.overallFraction(), progress.elapsed().toMillis(),
				progress.remaining().map(Duration::toMillis).orElse(-1L));
	}

	private static CrawlFailure from(final CrawlOperationFailure failure) {
		return new CrawlFailure(abbreviate(failure.type()), abbreviate(failure.message()));
	}

	private static String abbreviate(final String value) {
		return value.length() <= MAXIMUM_TEXT_LENGTH ? value : value.substring(0, MAXIMUM_TEXT_LENGTH - 1) + "…";
	}
}
