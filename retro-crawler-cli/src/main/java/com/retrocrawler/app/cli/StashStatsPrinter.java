package com.retrocrawler.app.cli;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;

import com.retrocrawler.core.stash.StashStats;

final class StashStatsPrinter {

	private StashStatsPrinter() {
		// Static utility class.
	}

	static void printToStdout(final StashStats stats) {
		Objects.requireNonNull(stats, "stats");
		System.out.println("Archives:  " + stats.archiveCount());
		System.out.println("Roots:     " + stats.rootCount());
		System.out.println("Nodes:     " + stats.nodeCount());
		System.out.println("Leaves:    " + stats.leafCount());
		System.out.println("Max depth: " + stats.maximumDepth());
		System.out.println();
		System.out.println("Gear by type:");
		stats.gearByType().entrySet().stream()
				.sorted(Comparator.comparing((Map.Entry<Class<?>, Long> entry) -> entry.getKey().getSimpleName())
						.thenComparing(entry -> entry.getKey().getName()))
				.forEach(
						entry -> System.out.println("  - " + entry.getKey().getSimpleName() + ": " + entry.getValue()));
	}
}
