package com.retrocrawler.core.archive;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.retrocrawler.core.progress.ProgressAccuracy;
import com.retrocrawler.core.progress.ProgressStage;
import com.retrocrawler.core.progress.Progressor;
import com.retrocrawler.core.util.PathNames;

final class ArchiveDigPlan {

	record Region(Path root, Path path) {
	}

	private final Map<Path, FolderListing> analyzedListings;

	private final Set<Region> regions;

	private final int analyzedDepth;

	private long completedRegions;

	private boolean progressStarted;

	ArchiveDigPlan(final Map<Path, FolderListing> analyzedListings, final List<Region> regions,
			final int analyzedDepth) {
		this.analyzedListings = new LinkedHashMap<>(analyzedListings);
		this.regions = new LinkedHashSet<>(regions);
		this.analyzedDepth = analyzedDepth;
	}

	Optional<FolderListing> listing(final Path path) {
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

	void reportCurrent(final Path path, final boolean insideRegion, final Progressor progressor) {
		final long current = Math.min(completedRegions + 1, totalRegions());
		final String prefix = insideRegion ? "Crawling archive region " + current + " of " + totalRegions() + ": "
				: "Crawling archive structure: ";
		final String message = prefix + PathNames.abbreviatePathName(path.toString());
		if (!progressStarted) {
			progressor.begin(ProgressStage.CRAWLING, message, totalRegions(), ProgressAccuracy.APPROXIMATE);
			progressStarted = true;
			return;
		}
		progressor.advanceTo(completedRegions, message);
	}

	void completeRegion(final Path path, final Progressor progressor) {
		completedRegions++;
		progressor.advanceTo(completedRegions, "Completed archive region " + completedRegions + " of " + totalRegions()
				+ ": " + PathNames.abbreviatePathName(path.toString()));
	}
}
