package com.retrocrawler.core.stash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveId;

class ArchiveCrawlTimesTest {

	private static final ArchiveId ARCHIVE_ID = ArchiveId.of("archive");

	private static final ArchiveDescriptor ARCHIVE = ArchiveDescriptor.of(ARCHIVE_ID, Path.of("archive"));

	private static final ARI ROOT = ARI.of("collection", ARCHIVE_ID, Path.of(""));

	private static final ARI SUBTREE = ARI.of("collection", ARCHIVE_ID, Path.of("shelf"));

	private static final Instant FULL_CRAWL_STARTED = Instant.parse("2026-08-29T08:00:00Z");

	private static final Instant FULL_CRAWL_OBSERVED = Instant.parse("2026-08-29T08:04:00Z");

	private static final Instant SUBTREE_CRAWL_STARTED = Instant.parse("2026-08-30T09:00:00Z");

	private static final Instant SUBTREE_OBSERVED = Instant.parse("2026-08-30T09:00:17Z");

	@Test
	void exposesCompleteAndSubtreeCrawlTimesThroughTheStash() {
		final Map<ARI, CrawlObservation> observations = new LinkedHashMap<>();
		observations.put(ROOT, new CrawlObservation(FULL_CRAWL_STARTED, FULL_CRAWL_OBSERVED));
		observations.put(SUBTREE, new CrawlObservation(SUBTREE_CRAWL_STARTED, SUBTREE_OBSERVED));
		final ArchiveCrawlTimes archiveTimes = new ArchiveCrawlTimes(ARCHIVE, observations);
		final Stash stash = new Stash(List.of(new ArchiveGear<>(ARCHIVE, List.of())), List.of(), List.of(archiveTimes));

		assertEquals(Optional.of(new CrawlObservation(FULL_CRAWL_STARTED, FULL_CRAWL_OBSERVED)),
				archiveTimes.completeCrawl());
		assertEquals(Optional.of(new CrawlObservation(SUBTREE_CRAWL_STARTED, SUBTREE_OBSERVED)),
				archiveTimes.observation(SUBTREE));
		assertEquals(Optional.of(Duration.ofMinutes(4)), archiveTimes.crawlDuration());
		assertEquals(List.of(ROOT, SUBTREE), archiveTimes.observations().keySet().stream().toList());
		assertEquals(List.of(archiveTimes), stash.crawlTimes());
		assertEquals(Optional.of(FULL_CRAWL_STARTED), stash.crawlStartedAt(ARCHIVE_ID));
		assertEquals(Optional.of(FULL_CRAWL_OBSERVED), stash.observedAt(ARCHIVE_ID));
		assertEquals(Optional.of(Duration.ofMinutes(4)), stash.crawlDuration(ARCHIVE_ID));
		assertEquals(Optional.of(SUBTREE_CRAWL_STARTED), stash.crawlStartedAt(SUBTREE));
		assertEquals(Optional.of(SUBTREE_OBSERVED), stash.observedAt(SUBTREE));
		assertEquals(Optional.empty(), stash.observedAt(ARI.of("collection", ARCHIVE_ID, Path.of("unknown"))));
	}

	@Test
	void allowsAProgrammaticallyConstructedStashToHaveUnknownCrawlTimes() {
		final Stash stash = new Stash(List.of(new ArchiveGear<>(ARCHIVE, List.of())));

		assertEquals(List.of(), stash.crawlTimes());
		assertEquals(Optional.empty(), stash.crawlStartedAt(ARCHIVE_ID));
		assertEquals(Optional.empty(), stash.observedAt(ARCHIVE_ID));
		assertEquals(Optional.empty(), stash.crawlDuration(ARCHIVE_ID));
	}

	@Test
	void rejectsTimestampsForAnotherArchiveOrWithoutTheArchiveRoot() {
		final ARI foreign = ARI.of("collection", ArchiveId.of("foreign"), Path.of(""));
		final CrawlObservation fullCrawl = new CrawlObservation(FULL_CRAWL_STARTED, FULL_CRAWL_OBSERVED);
		final ArchiveCrawlTimes times = new ArchiveCrawlTimes(ARCHIVE, Map.of(ROOT, fullCrawl));

		assertThrows(IllegalArgumentException.class, () -> new ArchiveCrawlTimes(ARCHIVE, Map.of(foreign, fullCrawl)));
		assertThrows(IllegalArgumentException.class, () -> new ArchiveCrawlTimes(ARCHIVE, Map.of(SUBTREE, fullCrawl)));
		assertThrows(IllegalArgumentException.class,
				() -> times.observation(ARI.of("another_collection", ARCHIVE_ID, Path.of(""))));
	}

	@Test
	void crawlObservationMapIsImmutable() {
		final CrawlObservation fullCrawl = new CrawlObservation(FULL_CRAWL_STARTED, FULL_CRAWL_OBSERVED);
		final ArchiveCrawlTimes times = new ArchiveCrawlTimes(ARCHIVE, Map.of(ROOT, fullCrawl));

		assertThrows(UnsupportedOperationException.class, () -> times.observations().put(SUBTREE, fullCrawl));
	}
}
