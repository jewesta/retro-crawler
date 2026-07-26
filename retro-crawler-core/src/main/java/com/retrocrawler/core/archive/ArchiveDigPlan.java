package com.retrocrawler.core.archive;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.retrocrawler.core.util.CrawlProgress;
import com.retrocrawler.core.util.Monitor;
import com.retrocrawler.core.util.PathNames;

final class ArchiveDigPlan {

	record Region(Path root, Path path) {
	}

	private final Map<Path, List<Path>> analyzedListings;

	private final Set<Region> regions;

	private final int analyzedDepth;

	private long completedRegions;

	ArchiveDigPlan(final Map<Path, List<Path>> analyzedListings, final List<Region> regions, final int analyzedDepth) {
		this.analyzedListings = new LinkedHashMap<>(analyzedListings);
		this.regions = new LinkedHashSet<>(regions);
		this.analyzedDepth = analyzedDepth;
	}

	Optional<List<Path>> listing(final Path path) {
		return Optional.ofNullable(analyzedListings.get(path));
	}

	boolean isRegionRoot(final Path root, final Path path) {
		return regions.contains(new Region(root, path));
	}

	long totalRegions() {
		return regions.size();
	}

	int analyzedDepth() {
		return analyzedDepth;
	}

	void reportCurrent(final Path path, final boolean insideRegion, final Monitor monitor) {
		final long current = Math.min(completedRegions + 1, totalRegions());
		final String prefix = insideRegion
				? "Crawling archive region " + current + " of " + totalRegions() + ": "
				: "Crawling archive structure: ";
		monitor.report(CrawlProgress.approximate(CrawlProgress.Phase.CRAWLING,
				prefix + PathNames.abbreviatePathName(path.toString()), completedRegions, totalRegions()));
	}

	void completeRegion(final Path path, final Monitor monitor) {
		completedRegions++;
		monitor.report(CrawlProgress.approximate(CrawlProgress.Phase.CRAWLING,
				"Completed archive region " + completedRegions + " of " + totalRegions() + ": "
						+ PathNames.abbreviatePathName(path.toString()),
				completedRegions, totalRegions()));
	}
}
