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
import com.retrocrawler.core.archive.clues.Clues;
import com.retrocrawler.core.archive.clues.FileNameClueFinder;
import com.retrocrawler.core.archive.filter.ArchivePathFilter;
import com.retrocrawler.core.archive.filter.IgnoreDotPaths;
import com.retrocrawler.core.archive.filter.IgnoreQNAPSystemPaths;
import com.retrocrawler.core.archive.filter.IgnoreWindowsSystemPaths;
import com.retrocrawler.core.archive.source.ArchiveSession;
import com.retrocrawler.core.progress.Progressor;

class ArchiveDiggerPathFilterTest {

	@TempDir
	private Path root;

	@Test
	void appliesAllFiltersToPlanningFilesAndPrunedSubtrees() throws IOException {
		Files.createFile(root.resolve("visible.txt"));
		Files.createFile(root.resolve(".DS_Store"));
		Files.createFile(root.resolve("Thumbs.db"));
		Files.createDirectory(root.resolve("Visible folder"));
		Files.createDirectories(root.resolve(".@__thumb/cache"));
		Files.createDirectories(root.resolve("@Recycle/deleted"));

		final ArchiveDefinition archive = definition(new IgnoreDotPaths(), new IgnoreWindowsSystemPaths(),
				new IgnoreQNAPSystemPaths());
		final ArchiveDigger digger = new ArchiveDigger(archive, new CrawlPlanning(2, 2, 100, Duration.ofMinutes(1)));
		final Progressor progressor = new Progressor();
		final ArchiveNode result;
		final ArchiveDigPlan plan;
		try (ArchiveSession session = digger.open(root)) {
			final ArchiveDigTarget target = digger.rootTarget(session);
			plan = digger.plan(List.of(target), progressor);
			result = digger.dig(target, plan, progressor);
		}

		assertEquals(1, plan.totalRegions());
		assertEquals(List.of("Visible folder"), result.children().stream().map(ArchiveNode::folder).toList());
		assertEquals(Set.of("visible.txt"), clue(result, "files").value());
	}

	@Test
	void acceptsEveryPathWhenNoFiltersAreConfigured() throws IOException {
		Files.createDirectory(root.resolve(".archive-metadata"));

		final ArchiveNode archive = new ArchiveDigger(definition()).dig(root, new Progressor());

		assertEquals(List.of(".archive-metadata"), archive.children().stream().map(ArchiveNode::folder).toList());
	}

	@Test
	void acceptsCollectionSpecificFilters() throws IOException {
		Files.createDirectory(root.resolve("admit"));
		Files.createDirectory(root.resolve("skip"));
		final ArchivePathFilter first = path -> true;
		final ArchivePathFilter second = path -> !"skip".equals(path.getFileName().toString());

		final ArchiveNode archive = new ArchiveDigger(definition(first, second)).dig(root, new Progressor());

		assertEquals(List.of("admit"), archive.children().stream().map(ArchiveNode::folder).toList());
	}

	private ArchiveDefinition definition(final ArchivePathFilter... filters) {
		return new TestArchiveDefinition(descriptor(), clueFinder(), List.of(filters));
	}

	private ArchiveDescriptor descriptor() {
		return new ArchiveDescriptor(ArchiveId.of("path_filter_test"), "Path filter test", root);
	}

	private static ArchivePathClueFinder clueFinder() {
		final FileNameClueFinder files = paths -> Clues
				.of(Clue.of("files", Set.copyOf(paths.stream().map(Path::getFileName).map(Path::toString).toList())));
		return new ArchivePathClueFinder(folder -> Clues.of(Clue.of("folder", folder)), List.of(), List.of(files));
	}

	private static Clue clue(final ArchiveNode node, final String key) {
		return node.artifact().clues().stream().filter(candidate -> key.equals(candidate.key())).findFirst()
				.orElseThrow();
	}
}
