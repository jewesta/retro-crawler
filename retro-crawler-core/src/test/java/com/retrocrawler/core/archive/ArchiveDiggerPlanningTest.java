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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.retrocrawler.core.CrawlProgressStages;
import com.retrocrawler.core.Journal;
import com.retrocrawler.core.archive.clues.ArchiveFolderClueFinder;
import com.retrocrawler.core.archive.clues.ArchiveNode;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.Clues;
import com.retrocrawler.core.archive.source.ArchiveSession;
import com.retrocrawler.core.progress.ProgressAccuracy;
import com.retrocrawler.core.progress.ProgressSnapshot;
import com.retrocrawler.core.progress.Progressor;

class ArchiveDiggerPlanningTest {

	@TempDir
	private Path root;

	@Test
	void expandsShallowLevelsUntilItHasEnoughApproximateRegions() throws IOException {
		createTree();
		final ArchiveDigger digger = digger(new CrawlPlanning(3, 5, 100, Duration.ofMinutes(1)));
		final List<ProgressSnapshot> events = new ArrayList<>();
		final Progressor progressor = Progressor.observing(progress -> events.add(progress.snapshot()));
		final Journal journal = new Journal(progressor);

		final ArchiveDigPlan plan;
		final ArchiveNode archive;
		try (ArchiveSession session = digger.open(root)) {
			final ArchiveDigTarget target = digger.rootTarget(session);
			plan = digger.plan(List.of(target), journal);
			archive = digger.dig(target, plan, journal);
		}

		assertEquals(2, plan.analyzedDepth());
		assertEquals(4, plan.totalRegions());
		assertEquals(2, archive.children().size());

		final List<ProgressSnapshot> crawling = events.stream()
				.filter(event -> event.stage().equals(CrawlProgressStages.CRAWLING)).toList();
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
		final Journal journal = new Journal();

		try (ArchiveSession session = digger.open(root)) {
			final ArchiveDigTarget target = digger.rootTarget(session);
			final ArchiveDigPlan plan = digger.plan(List.of(target), journal);
			final ArchiveDigTarget secondTarget = digger.target(session, second, journal).orElseThrow();

			assertEquals(2, plan.analyzedDepth());
			assertEquals(1, plan.totalRegions());
			assertTrue(plan.isRegionRoot(session, secondTarget.folder()));
		}
	}

	@Test
	void reusesListingsCollectedByTheAnalysisSweep() throws IOException {
		Files.createDirectory(root.resolve("known"));
		final ArchiveDigger digger = digger(new CrawlPlanning(2, 1, 100, Duration.ofMinutes(1)));
		final Journal journal = new Journal();
		final ArchiveNode archive;
		try (ArchiveSession session = digger.open(root)) {
			final ArchiveDigTarget target = digger.rootTarget(session);
			final ArchiveDigPlan plan = digger.plan(List.of(target), journal);
			Files.createDirectory(root.resolve("created-after-planning"));
			archive = digger.dig(target, plan, journal);
		}

		assertEquals(List.of("known"), archive.children().stream().map(ArchiveNode::folder).toList());
		assertFalse(archive.children().stream().anyMatch(node -> "created-after-planning".equals(node.folder())));
	}

	@Test
	void reusesEntryClassificationCollectedByTheAnalysisSweep() throws IOException {
		final Path changingEntry = Files.createFile(root.resolve("changing-entry"));
		final ArchiveDigger digger = digger(new CrawlPlanning(2, 1, 100, Duration.ofMinutes(1)));
		final Journal journal = new Journal();
		final ArchiveNode archive;
		try (ArchiveSession session = digger.open(root)) {
			final ArchiveDigTarget target = digger.rootTarget(session);
			final ArchiveDigPlan plan = digger.plan(List.of(target), journal);
			Files.delete(changingEntry);
			Files.createDirectory(changingEntry);
			archive = digger.dig(target, plan, journal);
		}

		assertTrue(archive.children() == null || archive.children().isEmpty());
	}

	private ArchiveDigger digger(final CrawlPlanning planning) {
		final ArchiveDescriptor descriptor = new ArchiveDescriptor(ArchiveId.of("planning_test"), "Planning test",
				root);
		final ArchiveFolderClueFinder clues = new ArchiveFolderClueFinder(folder -> Clues.of(Clue.of("folder", folder)),
				List.of(), List.of());
		return new ArchiveDigger(new TestArchiveDefinition(descriptor, clues), planning);
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
