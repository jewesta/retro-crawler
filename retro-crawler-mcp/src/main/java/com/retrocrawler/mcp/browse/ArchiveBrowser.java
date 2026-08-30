package com.retrocrawler.mcp.browse;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.retrocrawler.core.Journal;
import com.retrocrawler.core.RetroCrawler;
import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.stash.ArchiveCrawlTimes;
import com.retrocrawler.core.stash.ArchiveGear;
import com.retrocrawler.core.stash.CrawlObservation;
import com.retrocrawler.core.stash.GearNode;
import com.retrocrawler.core.stash.Stash;

/**
 * Browses bounded pages from the indexed folder tree of one immutable Stash.
 */
public final class ArchiveBrowser {

	public static final int DEFAULT_LIMIT = 50;

	public static final int MAXIMUM_LIMIT = 100;

	private static final int MAXIMUM_ARI_LENGTH = 4_000;

	private static final Path ROOT = Path.of("");

	private final RetroCrawler retroCrawler;

	private final Map<ArchiveId, ArchiveDescriptor> archives;

	public ArchiveBrowser(final RetroCrawler retroCrawler) {
		this.retroCrawler = Objects.requireNonNull(retroCrawler, "retroCrawler");
		final Map<ArchiveId, ArchiveDescriptor> registered = new LinkedHashMap<>();
		for (final ArchiveDescriptor archive : retroCrawler.archives()) {
			registered.put(archive.id(), archive);
		}
		this.archives = Map.copyOf(registered);
	}

	public ArchiveFolderPage browse(final String value, final Integer requestedOffset, final Integer requestedLimit)
			throws IOException {
		final ARI ari = ari(value);
		final int offset = offset(requestedOffset);
		final int limit = limit(requestedLimit);
		final Stash stash = retroCrawler.access(new Journal());
		final ArchiveDescriptor archive = archives.get(ari.archiveId());
		final ArchiveCrawlTimes crawlTimes = stash.crawlTimes().stream()
				.filter(candidate -> candidate.archive().id().equals(ari.archiveId())).findFirst()
				.orElseThrow(() -> new IllegalArgumentException(
						"Archive has no indexed folder observations: " + ari.archiveId()));
		final Map<ARI, CrawlObservation> observations = crawlTimes.observations();
		if (!observations.containsKey(ari)) {
			throw new IllegalArgumentException("Unknown indexed folder ARI: " + ari);
		}

		final FolderIndex index = new FolderIndex(archive, observations, archiveGear(stash, ari.archiveId()));
		final List<ARI> availableChildren = index.children(ari);
		final int from = Math.min(offset, availableChildren.size());
		final int to = Math.min(from + limit, availableChildren.size());
		final List<ArchiveFolderSummary> children = availableChildren.subList(from, to).stream().map(index::summary)
				.toList();
		return new ArchiveFolderPage(index.summary(ari), availableChildren.size(), offset, limit,
				to < availableChildren.size(), children);
	}

	private ARI ari(final String value) {
		final String text = Objects.requireNonNull(value, "ari");
		if (text.isBlank()) {
			throw new IllegalArgumentException("ari must not be blank.");
		}
		if (text.length() > MAXIMUM_ARI_LENGTH) {
			throw new IllegalArgumentException("ari must not exceed " + MAXIMUM_ARI_LENGTH + " characters.");
		}
		final ARI ari;
		try {
			ari = ARI.parse(text);
		} catch (final IllegalArgumentException failure) {
			throw new IllegalArgumentException("Invalid archive folder ARI: " + text, failure);
		}
		if (!retroCrawler.collectionId().equals(ari.collectionId())) {
			throw new IllegalArgumentException("Archive folder ARI belongs to collection '" + ari.collectionId()
					+ "' instead of '" + retroCrawler.collectionId() + "': " + ari);
		}
		if (!archives.containsKey(ari.archiveId())) {
			throw new IllegalArgumentException("Unknown archive id in folder ARI: " + ari.archiveId());
		}
		return ari;
	}

	private static int offset(final Integer requested) {
		if (requested == null) {
			return 0;
		}
		if (requested < 0) {
			throw new IllegalArgumentException("offset must not be negative.");
		}
		return requested;
	}

	private static int limit(final Integer requested) {
		if (requested == null) {
			return DEFAULT_LIMIT;
		}
		if (requested < 1 || requested > MAXIMUM_LIMIT) {
			throw new IllegalArgumentException("limit must be between 1 and " + MAXIMUM_LIMIT + ".");
		}
		return requested;
	}

	private static ArchiveGear<Object> archiveGear(final Stash stash, final ArchiveId archiveId) {
		return stash.archives().stream().filter(candidate -> candidate.archive().id().equals(archiveId)).findFirst()
				.orElseThrow(() -> new IllegalStateException("Indexed archive is absent from the Stash: " + archiveId));
	}

	private static final class FolderIndex {

		private final ArchiveDescriptor archive;

		private final Map<ARI, CrawlObservation> observations;

		private final Map<ARI, List<ARI>> children = new LinkedHashMap<>();

		private final Map<ARI, Long> subtreeGearCounts = new LinkedHashMap<>();

		private final Map<ARI, LinkedHashSet<String>> gearKinds = new LinkedHashMap<>();

		private FolderIndex(final ArchiveDescriptor archive, final Map<ARI, CrawlObservation> observations,
				final ArchiveGear<Object> gear) {
			this.archive = archive;
			this.observations = observations;
			for (final ARI folder : observations.keySet()) {
				if (!folder.resourcePath().toString().isEmpty()) {
					children.computeIfAbsent(parent(folder), ignored -> new ArrayList<>()).add(folder);
				}
			}
			for (final GearNode<Object> root : gear.roots()) {
				index(root);
			}
		}

		private List<ARI> children(final ARI folder) {
			return List.copyOf(children.getOrDefault(folder, List.of()));
		}

		private ArchiveFolderSummary summary(final ARI folder) {
			final CrawlObservation observation = observations.get(folder);
			if (observation == null) {
				throw new IllegalStateException("Indexed folder is missing its crawl observation: " + folder);
			}
			return new ArchiveFolderSummary(folder.toString(), name(folder), children(folder).size(),
					subtreeGearCounts.getOrDefault(folder, 0L),
					List.copyOf(gearKinds.getOrDefault(folder, new LinkedHashSet<>())),
					observation.crawlStartedAt().toString(), observation.observedAt().toString());
		}

		private void index(final GearNode<Object> node) {
			final ARI source = node.source();
			gearKinds.computeIfAbsent(source, ignored -> new LinkedHashSet<>()).add(gearKind(node.gear()));
			ARI current = source;
			while (true) {
				if (observations.containsKey(current)) {
					subtreeGearCounts.merge(current, 1L, Long::sum);
				}
				if (current.resourcePath().toString().isEmpty()) {
					break;
				}
				current = parent(current);
			}
			for (final GearNode<Object> child : node.children()) {
				index(child);
			}
		}

		private String name(final ARI folder) {
			return folder.resourcePath().toString().isEmpty() ? archive.name()
					: folder.resourcePath().getFileName().toString();
		}

		private static ARI parent(final ARI folder) {
			final Path parent = folder.resourcePath().getParent();
			return ARI.of(folder.collectionId(), folder.archiveId(), parent == null ? ROOT : parent);
		}

		private static String gearKind(final Object gear) {
			final String simpleName = gear.getClass().getSimpleName();
			return simpleName.isEmpty() ? gear.getClass().getName() : simpleName;
		}
	}
}
