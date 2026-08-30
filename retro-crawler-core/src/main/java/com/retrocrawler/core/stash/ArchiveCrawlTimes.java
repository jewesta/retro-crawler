package com.retrocrawler.core.stash;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.archive.ArchiveDescriptor;

/**
 * Crawl observations for the folder tree of one archive.
 * <p>
 * The root observation describes the last complete crawl of the archive. Every
 * observation identifies its crawl operation by its shared start time and also
 * records when that particular folder had been fully inspected. These times
 * describe the cached observations; they do not claim that the physical source
 * has remained unchanged since then.
 */
public final class ArchiveCrawlTimes {

	private final ArchiveDescriptor archive;

	private final Map<ARI, CrawlObservation> observations;

	private final String collectionId;

	private final Optional<CrawlObservation> completeCrawl;

	public ArchiveCrawlTimes(final ArchiveDescriptor archive, final Map<ARI, CrawlObservation> observations) {
		this.archive = Objects.requireNonNull(archive, "archive");
		Objects.requireNonNull(observations, "observations");
		final Map<ARI, CrawlObservation> copied = new LinkedHashMap<>();
		String collectionId = null;
		CrawlObservation rootObservation = null;
		for (final Map.Entry<ARI, CrawlObservation> entry : observations.entrySet()) {
			final ARI subtree = Objects.requireNonNull(entry.getKey(), "observations key");
			final CrawlObservation observation = Objects.requireNonNull(entry.getValue(), "observations value");
			if (!archive.id().equals(subtree.archiveId())) {
				throw new IllegalArgumentException("Crawl observation belongs to archive '" + subtree.archiveId()
						+ "' instead of '" + archive.id() + "'.");
			}
			if (collectionId == null) {
				collectionId = subtree.collectionId();
			} else if (!collectionId.equals(subtree.collectionId())) {
				throw new IllegalArgumentException("Crawl observations must belong to one collection.");
			}
			if (subtree.resourcePath().toString().isEmpty()) {
				rootObservation = observation;
			}
			copied.put(subtree, observation);
		}
		if (!copied.isEmpty() && rootObservation == null) {
			throw new IllegalArgumentException("Crawl observations must include the archive root.");
		}
		this.observations = Collections.unmodifiableMap(copied);
		this.collectionId = collectionId;
		this.completeCrawl = Optional.ofNullable(rootObservation);
	}

	public ArchiveDescriptor archive() {
		return archive;
	}

	/** The last complete archive crawl, if known. */
	public Optional<CrawlObservation> completeCrawl() {
		return completeCrawl;
	}

	/** The most recent crawl observation of this exact subtree, if known. */
	public Optional<CrawlObservation> observation(final ARI subtree) {
		Objects.requireNonNull(subtree, "subtree");
		if (!archive.id().equals(subtree.archiveId())) {
			throw new IllegalArgumentException(
					"ARI belongs to archive '" + subtree.archiveId() + "' instead of '" + archive.id() + "'.");
		}
		if (collectionId != null && !collectionId.equals(subtree.collectionId())) {
			throw new IllegalArgumentException(
					"ARI belongs to collection '" + subtree.collectionId() + "' instead of '" + collectionId + "'.");
		}
		return Optional.ofNullable(observations.get(subtree));
	}

	/**
	 * Duration of the last complete archive crawl, calculated from its root
	 * observation.
	 */
	public Optional<Duration> crawlDuration() {
		return completeCrawl.map(CrawlObservation::elapsed);
	}

	/**
	 * Every indexed folder ARI and its most recent observation, in tree order.
	 */
	public Map<ARI, CrawlObservation> observations() {
		return observations;
	}
}
