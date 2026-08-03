package com.retrocrawler.core.archive;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.retrocrawler.core.archive.clues.ArchiveNode;
import com.retrocrawler.core.archive.clues.ArchivePathClueFinder;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.FileNameClueFinder;
import com.retrocrawler.core.progress.Progressor;

class ArchiveDiggerCrawlPolicyTest {

	@TempDir
	private Path root;

	@Test
	void appliesOnePolicyToPlanningFilesAndPrunedSubtrees() throws IOException {
		Files.createFile(root.resolve("visible.txt"));
		Files.createFile(root.resolve(".DS_Store"));
		Files.createFile(root.resolve("Thumbs.db"));
		Files.createDirectory(root.resolve("Visible folder"));
		Files.createDirectories(root.resolve(".@__thumb/cache"));
		Files.createDirectories(root.resolve("@Recycle/deleted"));

		final ArchiveDigger digger = new ArchiveDigger(descriptor(), clueFinder(),
				new CrawlPlanning(2, 2, 100, Duration.ofMinutes(1)), new IgnoreSystemFiles());
		final Progressor progressor = new Progressor();
		final ArchiveDigPlan plan = digger.plan(List.of(new ArchiveDigTarget(root, root)), progressor);
		final ArchiveNode archive = digger.dig(root, plan, progressor);

		assertEquals(1, plan.totalRegions());
		assertEquals(List.of("Visible folder"), archive.getChildren().stream().map(ArchiveNode::getFolder).toList());
		assertEquals(Set.of("visible.txt"), clue(archive, "files").getValue());
	}

	@Test
	void preservesExistingIncludeEverythingBehaviorByDefault() throws IOException {
		Files.createDirectory(root.resolve(".archive-metadata"));

		final ArchiveNode archive = new ArchiveDigger(descriptor(), clueFinder())
				.dig(root, new Progressor());

		assertEquals(List.of(".archive-metadata"),
				archive.getChildren().stream().map(ArchiveNode::getFolder).toList());
	}

	@Test
	void acceptsCollectionSpecificPolicies() throws IOException {
		Files.createDirectory(root.resolve("admit"));
		Files.createDirectory(root.resolve("skip"));
		final CrawlPolicy policy = path -> !"skip".equals(path.getFileName().toString());

		final ArchiveNode archive = new ArchiveDigger(descriptor(), clueFinder(), CrawlPlanning.defaults(), policy)
				.dig(root, new Progressor());

		assertEquals(List.of("admit"), archive.getChildren().stream().map(ArchiveNode::getFolder).toList());
	}

	private ArchiveDescriptor descriptor() {
		return new ArchiveDescriptor(ArchiveId.of("crawl_policy_test"), "Crawl policy test", List.of(root));
	}

	private static ArchivePathClueFinder clueFinder() {
		final FileNameClueFinder files = paths -> Set.of(Clue.of("files",
				Set.copyOf(paths.stream().map(Path::getFileName).map(Path::toString).toList())));
		return new ArchivePathClueFinder(folder -> Set.of(Clue.of("folder", folder)), List.of(), List.of(files));
	}

	private static Clue clue(final ArchiveNode node, final String key) {
		return node.getArtifact().getClues().stream()
				.filter(candidate -> key.equals(candidate.getKey()))
				.findFirst()
				.orElseThrow();
	}
}
