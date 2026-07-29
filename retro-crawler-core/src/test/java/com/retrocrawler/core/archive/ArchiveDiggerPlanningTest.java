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
import com.retrocrawler.core.progress.ProgressAccuracy;
import com.retrocrawler.core.progress.ProgressSnapshot;
import com.retrocrawler.core.progress.ProgressStage;
import com.retrocrawler.core.progress.Progressor;

class ArchiveDiggerPlanningTest {

	@TempDir
	private Path root;

	@Test
	void expandsShallowLevelsUntilItHasEnoughApproximateRegions() throws IOException {
		createTree();
		final ArchiveDigger digger = digger(new CrawlPlanning(3, 5, 100, Duration.ofMinutes(1)));
		final List<ProgressSnapshot> events = new ArrayList<>();
		final Progressor progressor = Progressor.observing(events::add);

		final ArchiveDigPlan plan = digger.plan(List.of(new ArchiveDigTarget(root, root)), progressor);
		final ArchiveNode archive = digger.dig(root, plan, progressor);

		assertEquals(2, plan.analyzedDepth());
		assertEquals(4, plan.totalRegions());
		assertEquals(2, archive.getChildren().size());

		final List<ProgressSnapshot> crawling = events.stream()
				.filter(event -> event.stage().equals(ProgressStage.CRAWLING)).toList();
		final ProgressSnapshot last = crawling.getLast();
		assertEquals(ProgressAccuracy.APPROXIMATE, last.accuracy());
		assertEquals(4, last.completed());
		assertEquals(4, last.total());
	}

	@Test
	void boundsPlanningDepthForADeepNarrowTree() throws IOException {
		final Path first = Files.createDirectory(root.resolve("one"));
		final Path second = Files.createDirectory(first.resolve("two"));
		Files.createDirectory(second.resolve("three"));
		final ArchiveDigger digger = digger(new CrawlPlanning(100, 2, 100, Duration.ofMinutes(1)));

		final ArchiveDigPlan plan = digger.plan(List.of(new ArchiveDigTarget(root, root)), new Progressor());

		assertEquals(2, plan.analyzedDepth());
		assertEquals(1, plan.totalRegions());
		assertTrue(plan.isRegionRoot(root, second));
	}

	@Test
	void reusesListingsCollectedByTheAnalysisSweep() throws IOException {
		Files.createDirectory(root.resolve("known"));
		final ArchiveDigger digger = digger(new CrawlPlanning(2, 1, 100, Duration.ofMinutes(1)));
		final Progressor progressor = new Progressor();
		final ArchiveDigPlan plan = digger.plan(List.of(new ArchiveDigTarget(root, root)), progressor);
		Files.createDirectory(root.resolve("created-after-planning"));

		final ArchiveNode archive = digger.dig(root, plan, progressor);

		assertEquals(List.of("known"), archive.getChildren().stream().map(ArchiveNode::getFolder).toList());
		assertFalse(archive.getChildren().stream()
				.anyMatch(node -> "created-after-planning".equals(node.getFolder())));
	}

	@Test
	void reusesEntryClassificationCollectedByTheAnalysisSweep() throws IOException {
		final Path changingEntry = Files.createFile(root.resolve("changing-entry"));
		final ArchiveDigger digger = digger(new CrawlPlanning(2, 1, 100, Duration.ofMinutes(1)));
		final Progressor progressor = new Progressor();
		final ArchiveDigPlan plan = digger.plan(List.of(new ArchiveDigTarget(root, root)), progressor);
		Files.delete(changingEntry);
		Files.createDirectory(changingEntry);

		final ArchiveNode archive = digger.dig(root, plan, progressor);

		assertTrue(archive.getChildren() == null || archive.getChildren().isEmpty());
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
