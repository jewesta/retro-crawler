package com.retrocrawler.app.cli;

import java.util.Map;
import java.util.Objects;

import com.retrocrawler.core.GearArchive;
import com.retrocrawler.core.GearBucket;
import com.retrocrawler.core.GearNode;

final class ArchiveStats {

	private int bucketCount;
	private int rootCount;
	private int nodeCount;
	private int leafCount;
	private int maxDepth;
	private final Map<String, Integer> byType;

	private ArchiveStats(final Map<String, Integer> byType) {
		this.byType = byType;
	}

	static <G> ArchiveStats compute(final GearArchive<G> archive) {
		Objects.requireNonNull(archive, "archive");

		final Map<String, Integer> byType = new java.util.TreeMap<>();
		final ArchiveStats out = new ArchiveStats(byType);

		out.bucketCount = archive.getBuckets().size();

		for (final GearBucket<G> bucket : archive.getBuckets()) {
			out.rootCount += bucket.getRoots().size();
			for (final GearNode<G> root : bucket.getRoots()) {
				walk(root, 1, out, byType);
			}
		}

		return out;
	}

	private static <G> void walk(final GearNode<G> node, final int depth, final ArchiveStats stats,
			final Map<String, Integer> byType) {

		stats.nodeCount++;
		stats.maxDepth = Math.max(stats.maxDepth, depth);

		final G gear = node.getGear();
		final String typeName = gear == null ? "null" : gear.getClass().getSimpleName();
		byType.merge(typeName, 1, Integer::sum);

		final var children = node.getChildren();
		if (children.isEmpty()) {
			stats.leafCount++;
			return;
		}

		for (final GearNode<G> c : children) {
			walk(c, depth + 1, stats, byType);
		}
	}

	void printToStdout() {
		System.out.println("Buckets:   " + bucketCount);
		System.out.println("Roots:     " + rootCount);
		System.out.println("Nodes:     " + nodeCount);
		System.out.println("Leaves:    " + leafCount);
		System.out.println("Max depth: " + maxDepth);
		System.out.println();
		System.out.println("Gear by type:");
		for (final var e : byType.entrySet()) {
			System.out.println("  - " + e.getKey() + ": " + e.getValue());
		}
	}
}