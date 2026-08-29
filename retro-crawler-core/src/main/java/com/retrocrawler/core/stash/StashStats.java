package com.retrocrawler.core.stash;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Structural statistics for a resolved {@link Stash}.
 */
public record StashStats(long archiveCount, long rootCount, long nodeCount, long leafCount, int maximumDepth,
		Map<Class<?>, Long> gearByType) {

	public StashStats {
		if (archiveCount < 0 || rootCount < 0 || nodeCount < 0 || leafCount < 0 || maximumDepth < 0) {
			throw new IllegalArgumentException("Stash statistics must not be negative.");
		}
		gearByType = Map.copyOf(Objects.requireNonNull(gearByType, "gearByType"));
	}

	public static StashStats from(final Stash stash) {
		Objects.requireNonNull(stash, "stash");

		final MutableStats stats = new MutableStats();
		stats.archiveCount = stash.archives().size();
		for (final ArchiveGear<?> archive : stash.archives()) {
			stats.rootCount += archive.roots().size();
			for (final GearNode<?> root : archive.roots()) {
				walk(root, 1, stats);
			}
		}

		return new StashStats(stats.archiveCount, stats.rootCount, stats.nodeCount, stats.leafCount, stats.maximumDepth,
				stats.gearByType);
	}

	private static void walk(final GearNode<?> node, final int depth, final MutableStats stats) {
		stats.nodeCount++;
		stats.maximumDepth = Math.max(stats.maximumDepth, depth);
		stats.gearByType.merge(node.gear().getClass(), 1L, Long::sum);

		if (node.children().isEmpty()) {
			stats.leafCount++;
			return;
		}
		for (final GearNode<?> child : node.children()) {
			walk(child, depth + 1, stats);
		}
	}

	private static final class MutableStats {

		private long archiveCount;
		private long rootCount;
		private long nodeCount;
		private long leafCount;
		private int maximumDepth;
		private final Map<Class<?>, Long> gearByType = new LinkedHashMap<>();
	}
}
