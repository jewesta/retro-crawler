package com.retrocrawler.core.stash;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.gear.filter.FilterAvailability;
import com.retrocrawler.core.gear.filter.FilterDefinition;
import com.retrocrawler.core.gear.filter.FilterDefinitions;

/**
 * The complete immutable, model-dependent arrangement of resolved Gear.
 *
 * <p>
 * A stash is derived from the model-independent clue archives. It is a runtime
 * view rather than the collection's source of truth. Its gear stays grouped by
 * the archive it was found in, so every result keeps its provenance.
 */
public final class Stash implements FilterDefinitions {

	private final List<ArchiveGear<Object>> archives;

	private final List<ArchiveCrawlTimes> crawlTimes;

	private final Map<ArchiveId, ArchiveCrawlTimes> crawlTimesByArchive;

	private final FilterAvailabilityIndex filterAvailability;

	public Stash(final List<ArchiveGear<Object>> archives) {
		this(archives, List.of());
	}

	public Stash(final List<ArchiveGear<Object>> archives, final List<FilterDefinition<?>> filters) {
		this(archives, filters, List.of());
	}

	public Stash(final List<ArchiveGear<Object>> archives, final List<FilterDefinition<?>> filters,
			final List<ArchiveCrawlTimes> crawlTimes) {
		this.archives = List.copyOf(Objects.requireNonNull(archives, "archives"));
		Objects.requireNonNull(crawlTimes, "crawlTimes");
		final Map<ArchiveId, ArchiveCrawlTimes> suppliedTimes = new LinkedHashMap<>();
		for (final ArchiveCrawlTimes times : crawlTimes) {
			final ArchiveCrawlTimes previous = suppliedTimes.putIfAbsent(times.archive().id(), times);
			if (previous != null) {
				throw new IllegalArgumentException("Duplicate crawl times for archive: " + times.archive().id());
			}
		}
		final List<ArchiveCrawlTimes> orderedTimes = new ArrayList<>();
		final List<GearNode<Object>> roots = new ArrayList<>();
		for (final ArchiveGear<Object> archive : this.archives) {
			roots.addAll(archive.roots());
			final ArchiveCrawlTimes times = suppliedTimes.remove(archive.archive().id());
			if (times != null) {
				if (!archive.archive().equals(times.archive())) {
					throw new IllegalArgumentException(
							"Crawl times do not describe configured archive: " + archive.archive().id());
				}
				orderedTimes.add(times);
			}
		}
		if (!suppliedTimes.isEmpty()) {
			throw new IllegalArgumentException("Crawl times describe an archive absent from the Stash: "
					+ suppliedTimes.keySet().iterator().next());
		}
		this.crawlTimes = List.copyOf(orderedTimes);
		this.crawlTimesByArchive = this.crawlTimes.stream()
				.collect(Collectors.toUnmodifiableMap(times -> times.archive().id(), times -> times));
		filterAvailability = new FilterAvailabilityIndex(filters, roots);
	}

	/** Every archive and its natural Gear hierarchy, in composition order. */
	public List<ArchiveGear<Object>> archives() {
		return archives;
	}

	/** Calculates structural statistics for this immutable Stash. */
	public StashStats stats() {
		return StashStats.from(this);
	}

	/** Crawl observations for each archive carrying known crawl metadata. */
	public List<ArchiveCrawlTimes> crawlTimes() {
		return crawlTimes;
	}

	/** When the last complete crawl of one archive started, if known. */
	public Optional<Instant> crawlStartedAt(final ArchiveId archiveId) {
		Objects.requireNonNull(archiveId, "archiveId");
		return Optional.ofNullable(crawlTimesByArchive.get(archiveId)).flatMap(ArchiveCrawlTimes::completeCrawl)
				.map(CrawlObservation::crawlStartedAt);
	}

	/** When the crawl containing this exact subtree started, if known. */
	public Optional<Instant> crawlStartedAt(final ARI subtree) {
		Objects.requireNonNull(subtree, "subtree");
		return Optional.ofNullable(crawlTimesByArchive.get(subtree.archiveId()))
				.flatMap(times -> times.observation(subtree)).map(CrawlObservation::crawlStartedAt);
	}

	/** When the root was fully observed during its last complete crawl. */
	public Optional<Instant> observedAt(final ArchiveId archiveId) {
		Objects.requireNonNull(archiveId, "archiveId");
		return Optional.ofNullable(crawlTimesByArchive.get(archiveId)).flatMap(ArchiveCrawlTimes::completeCrawl)
				.map(CrawlObservation::observedAt);
	}

	/**
	 * When this exact subtree was fully observed during its most recent crawl.
	 */
	public Optional<Instant> observedAt(final ARI subtree) {
		Objects.requireNonNull(subtree, "subtree");
		return Optional.ofNullable(crawlTimesByArchive.get(subtree.archiveId()))
				.flatMap(times -> times.observation(subtree)).map(CrawlObservation::observedAt);
	}

	/** Duration of the last complete physical crawl of one archive. */
	public Optional<Duration> crawlDuration(final ArchiveId archiveId) {
		Objects.requireNonNull(archiveId, "archiveId");
		return Optional.ofNullable(crawlTimesByArchive.get(archiveId)).flatMap(ArchiveCrawlTimes::crawlDuration);
	}

	@Override
	public List<FilterDefinition<?>> filters() {
		return filterAvailability.filters();
	}

	/**
	 * Lazily computes this complete collection's availability for one filter.
	 */
	@Override
	public <T> FilterAvailability<T> availability(final FilterDefinition<T> filter) {
		return filterAvailability.availability(filter);
	}

	/** Pulls every recognized Gear occurrence from this Stash. */
	public Batch<Object> pull() {
		return query(Object.class).pull();
	}

	/**
	 * Starts an immutable query selecting occurrences assignable to the type.
	 */
	public <G> Query<G> query(final Class<G> gearType) {
		return Query.from(this, gearType);
	}

}
