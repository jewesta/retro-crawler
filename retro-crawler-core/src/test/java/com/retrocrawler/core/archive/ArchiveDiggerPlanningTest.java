package com.retrocrawler.core.archive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.retrocrawler.core.archive.clues.ArchiveNode;
import com.retrocrawler.core.archive.clues.ArchivePathClueFinder;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.util.CrawlProgress;
import com.retrocrawler.core.util.Monitor;

class ArchiveDiggerPlanningTest {

	@TempDir
	private Path root;

	@Test
	void expandsShallowLevelsUntilItHasEnoughApproximateRegions() throws IOException {
		createTree();
		final ArchiveDigger digger = digger(new CrawlPlanning(3, 5, 100, Duration.ofMinutes(1)));
		final List<CrawlProgress> events = new ArrayList<>();
		final Monitor monitor = Monitor.observing(events::add);

		final ArchiveDigPlan plan = digger.plan(List.of(root), monitor);
		final ArchiveNode archive = digger.dig(root, plan, monitor);

		assertEquals(2, plan.analyzedDepth());
		assertEquals(4, plan.totalRegions());
		assertEquals(2, archive.getChildren().size());

		final List<CrawlProgress> crawling = events.stream()
				.filter(event -> event.phase() == CrawlProgress.Phase.CRAWLING).toList();
		final CrawlProgress last = crawling.getLast();
		assertTrue(last.approximate());
		assertEquals(4, last.completed());
		assertEquals(4, last.total());
	}

	@Test
	void boundsPlanningDepthForADeepNarrowTree() throws IOException {
		final Path first = Files.createDirectory(root.resolve("one"));
		final Path second = Files.createDirectory(first.resolve("two"));
		Files.createDirectory(second.resolve("three"));
		final ArchiveDigger digger = digger(new CrawlPlanning(100, 2, 100, Duration.ofMinutes(1)));

		final ArchiveDigPlan plan = digger.plan(List.of(root), new Monitor(message -> {
			// No progress output required.
		}));

		assertEquals(2, plan.analyzedDepth());
		assertEquals(1, plan.totalRegions());
		assertTrue(plan.isRegionRoot(root, second));
	}

	@Test
	void reusesListingsCollectedByTheAnalysisSweep() throws IOException {
		Files.createDirectory(root.resolve("known"));
		final ArchiveDigger digger = digger(new CrawlPlanning(2, 1, 100, Duration.ofMinutes(1)));
		final Monitor monitor = new Monitor(message -> {
			// No progress output required.
		});
		final ArchiveDigPlan plan = digger.plan(List.of(root), monitor);
		Files.createDirectory(root.resolve("created-after-planning"));

		final ArchiveNode archive = digger.dig(root, plan, monitor);

		assertEquals(List.of("known"), archive.getChildren().stream().map(ArchiveNode::getFolder).toList());
		assertFalse(archive.getChildren().stream()
				.anyMatch(node -> "created-after-planning".equals(node.getFolder())));
	}

	private ArchiveDigger digger(final CrawlPlanning planning) {
		final ArchiveDescriptor descriptor = new ArchiveDescriptor(ArchiveId.of("planning_test"), "Planning test",
				List.of(root));
		final ArchivePathClueFinder clues = new ArchivePathClueFinder(
				folder -> Set.of(Clue.of("folder", folder)), List.of(), List.of());
		return new ArchiveDigger(descriptor, clues, planning);
	}

	private void createTree() throws IOException {
		final Path first = Files.createDirectory(root.resolve("first"));
		Files.createDirectory(first.resolve("one"));
		Files.createDirectory(first.resolve("two"));
		final Path second = Files.createDirectory(root.resolve("second"));
		Files.createDirectory(second.resolve("three"));
		Files.createDirectory(second.resolve("four"));
	}
}
