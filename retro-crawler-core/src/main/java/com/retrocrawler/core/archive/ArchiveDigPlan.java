package com.retrocrawler.core.archive;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.retrocrawler.core.CrawlProgressStages;
import com.retrocrawler.core.Journal;
import com.retrocrawler.core.archive.source.ArchiveFolder;
import com.retrocrawler.core.archive.source.ArchiveSession;
import com.retrocrawler.core.progress.ProgressAccuracy;
import com.retrocrawler.core.util.PathNames;

final class ArchiveDigPlan {

	record Region(ArchiveSession session, ArchiveFolder root, ArchiveFolder folder) {

		FolderKey key() {
			return new FolderKey(session, folder.path());
		}
	}

	static final class FolderKey {

		private final ArchiveSession session;

		private final Path path;

		FolderKey(final ArchiveSession session, final Path path) {
			this.session = Objects.requireNonNull(session, "session");
			this.path = Objects.requireNonNull(path, "path").normalize();
		}

		@Override
		public boolean equals(final Object object) {
			return object instanceof FolderKey other && session == other.session && path.equals(other.path);
		}

		@Override
		public int hashCode() {
			return 31 * System.identityHashCode(session) + path.hashCode();
		}
	}

	private final Map<FolderKey, FolderListing> analyzedListings;

	private final Set<FolderKey> regions;

	private final int analyzedDepth;

	private long completedRegions;

	private boolean progressStarted;

	ArchiveDigPlan(final Map<FolderKey, FolderListing> analyzedListings, final List<Region> regions,
			final int analyzedDepth) {
		this.analyzedListings = new LinkedHashMap<>(analyzedListings);
		this.regions = regions.stream().map(Region::key)
				.collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
		this.analyzedDepth = analyzedDepth;
	}

	Optional<FolderListing> listing(final ArchiveSession session, final ArchiveFolder folder) {
		return Optional.ofNullable(analyzedListings.get(new FolderKey(session, folder.path())));
	}

	boolean isRegionRoot(final ArchiveSession session, final ArchiveFolder folder) {
		return regions.contains(new FolderKey(session, folder.path()));
	}

	long totalRegions() {
		return regions.size();
	}

	int analyzedDepth() {
		return analyzedDepth;
	}

	void reportCurrent(final ArchiveFolder folder, final boolean insideRegion, final Journal journal) {
		final long current = Math.min(completedRegions + 1, totalRegions());
		final String prefix = insideRegion ? "Crawling archive region " + current + " of " + totalRegions() + ": "
				: "Crawling archive structure: ";
		final String message = prefix + PathNames.abbreviatePathName(folder.path().toString());
		if (!progressStarted) {
			journal.begin(CrawlProgressStages.CRAWLING, message, totalRegions(), ProgressAccuracy.APPROXIMATE);
			progressStarted = true;
			return;
		}
		journal.advanceTo(completedRegions, message);
	}

	void completeRegion(final ArchiveFolder folder, final Journal journal) {
		completedRegions++;
		journal.advanceTo(completedRegions, "Completed archive region " + completedRegions + " of " + totalRegions()
				+ ": " + PathNames.abbreviatePathName(folder.path().toString()));
	}
}
