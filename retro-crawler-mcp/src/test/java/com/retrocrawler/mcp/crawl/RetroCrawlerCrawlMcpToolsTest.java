package com.retrocrawler.mcp.crawl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.retrocrawler.core.RetroCrawler;
import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.archive.ReindexScope;
import com.retrocrawler.core.crawl.CrawlOperation;
import com.retrocrawler.core.crawl.CrawlOperationFailure;
import com.retrocrawler.core.crawl.CrawlOperationId;
import com.retrocrawler.core.crawl.CrawlOperationService;
import com.retrocrawler.core.crawl.CrawlOperationState;
import com.retrocrawler.core.progress.ProgressAccuracy;
import com.retrocrawler.core.progress.ProgressSnapshot;
import com.retrocrawler.core.progress.ProgressStage;
import com.retrocrawler.core.progress.ProgressState;

class RetroCrawlerCrawlMcpToolsTest {

	private static final String COLLECTION_ID = "test_collection";

	private static final ArchiveDescriptor IBM = archive("ibm");

	private static final ArchiveDescriptor OTHER = archive("other");

	private CrawlOperationService operations;

	private RetroCrawlerCrawlMcpTools tools;

	@BeforeEach
	void setUp() {
		final RetroCrawler crawler = mock(RetroCrawler.class);
		when(crawler.collectionId()).thenReturn(COLLECTION_ID);
		when(crawler.archives()).thenReturn(List.of(IBM, OTHER));
		operations = mock(CrawlOperationService.class);
		tools = new RetroCrawlerCrawlMcpTools(crawler, operations);
	}

	@Test
	void startsAFullAsynchronousCrawlWhenNoArchivesAreSelected() {
		when(operations.start(any())).thenAnswer(invocation -> operation(invocation.getArgument(0)));

		final CrawlStatus status = tools.startCrawl(null);

		final ArgumentCaptor<ReindexScope> scope = ArgumentCaptor.forClass(ReindexScope.class);
		verify(operations).start(scope.capture());
		assertThat(scope.getValue().kind()).isEqualTo(ReindexScope.Kind.ALL);
		assertThat(status.id()).isEqualTo("operation-1");
		assertThat(status.state()).isEqualTo("RUNNING");
		assertThat(status.active()).isTrue();
		assertThat(status.finishedAt()).isEmpty();
		assertThat(status.progress()).isEqualTo(new CrawlProgress("CRAWLING", "Finding clues", 3, 10, "APPROXIMATE",
				"RUNNING", 0.3, 0.15, 4_000, 8_000));
	}

	@Test
	void mapsSelectedLogicalArchivesToCanonicalRootAris() {
		when(operations.start(any())).thenAnswer(invocation -> operation(invocation.getArgument(0)));

		tools.startCrawl(List.of("other", "ibm"));

		final ArgumentCaptor<ReindexScope> scope = ArgumentCaptor.forClass(ReindexScope.class);
		verify(operations).start(scope.capture());
		assertThat(scope.getValue().kind()).isEqualTo(ReindexScope.Kind.SUBTREES);
		assertThat(scope.getValue().subtrees()).containsExactly(ARI.of(COLLECTION_ID, OTHER.id(), Path.of("")),
				ARI.of(COLLECTION_ID, IBM.id(), Path.of("")));
	}

	@Test
	void rejectsUnknownAndDuplicateArchiveIdsBeforeStarting() {
		assertThatIllegalArgumentException().isThrownBy(() -> tools.startCrawl(List.of("missing")))
				.withMessage("Unknown archive id: missing");
		assertThatIllegalArgumentException().isThrownBy(() -> tools.startCrawl(List.of("ibm", "ibm")))
				.withMessage("archiveIds must not contain duplicate ID: ibm");
	}

	@Test
	void getsAndCancelsRetainedOperations() {
		final CrawlOperation operation = operation(ReindexScope.all());
		when(operations.operation(operation.id())).thenReturn(Optional.of(operation));
		when(operations.cancel(operation.id())).thenReturn(operation);

		assertThat(tools.getCrawl(" operation-1 ").id()).isEqualTo("operation-1");
		assertThat(tools.cancelCrawl("operation-1").id()).isEqualTo("operation-1");
		verify(operations).cancel(operation.id());
	}

	@Test
	void rejectsAnUnknownOperation() {
		when(operations.operation(new CrawlOperationId("missing"))).thenReturn(Optional.empty());

		assertThatIllegalArgumentException().isThrownBy(() -> tools.getCrawl("missing"))
				.withMessage("Unknown crawl operation: missing");
	}

	private static CrawlOperation operation(final ReindexScope scope) {
		final ProgressSnapshot progress = new ProgressSnapshot("progress-1", ProgressStage.of("crawling"),
				"Finding clues", 3, 10, ProgressAccuracy.APPROXIMATE, ProgressState.RUNNING, 0.3, 0.15,
				Duration.ofSeconds(4), Optional.of(Duration.ofSeconds(8)));
		return new CrawlOperation(new CrawlOperationId("operation-1"), scope, CrawlOperationState.RUNNING,
				Instant.parse("2026-08-30T12:00:00Z"), Optional.empty(), progress,
				List.of(new CrawlOperationFailure("example.Failure", "example failure")));
	}

	private static ArchiveDescriptor archive(final String id) {
		return new ArchiveDescriptor(ArchiveId.of(id), id, Path.of(id));
	}
}
