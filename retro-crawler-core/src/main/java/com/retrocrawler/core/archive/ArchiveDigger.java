package com.retrocrawler.core.archive;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.retrocrawler.core.archive.clues.ArchiveNode;
import com.retrocrawler.core.archive.clues.ArchivePathClueFinder;
import com.retrocrawler.core.archive.clues.Artifact;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.util.CrawlProgress;
import com.retrocrawler.core.util.Hashes;
import com.retrocrawler.core.util.Monitor;

public class ArchiveDigger {

	private static final Logger logger = LoggerFactory.getLogger(ArchiveDigger.class);

	private final ArchiveDescriptor descriptor;

	private final ArchivePathClueFinder clueFinder;

	private final CrawlPlanning planning;

	public ArchiveDigger(final ArchiveDescriptor descriptor, final ArchivePathClueFinder clueFinder) {
		this(descriptor, clueFinder, CrawlPlanning.defaults());
	}

	public ArchiveDigger(final ArchiveDescriptor descriptor, final ArchivePathClueFinder clueFinder,
			final CrawlPlanning planning) {
		this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
		this.clueFinder = Objects.requireNonNull(clueFinder, "clueFinder");
		this.planning = Objects.requireNonNull(planning, "planning");
	}

	public ArchiveNode dig(final Path path, final Monitor monitor) throws IOException {
		final ArchiveDigPlan plan = plan(List.of(path), monitor);
		return dig(path, plan, monitor);
	}

	ArchiveDigPlan plan(final Collection<Path> roots, final Monitor monitor) throws IOException {
		Objects.requireNonNull(roots, "roots");
		Objects.requireNonNull(monitor, "monitor");
		monitor.throwIfCancelled();

		final List<ArchiveDigPlan.Region> initialRegions = new ArrayList<>();
		for (final Path root : roots) {
			if (!Files.isDirectory(root)) {
				throw new IllegalArgumentException("Expected a folder but got: " + root);
			}
			initialRegions.add(new ArchiveDigPlan.Region(root, root));
		}
		if (initialRegions.isEmpty()) {
			throw new IllegalArgumentException("At least one archive root is required.");
		}

		final long started = System.nanoTime();
		final Map<Path, List<Path>> analyzedListings = new LinkedHashMap<>();
		List<ArchiveDigPlan.Region> frontier = initialRegions;
		int depth = 0;
		reportPlanning(monitor, depth, frontier.size(), false);

		while (frontier.size() < planning.targetRegions() && depth < planning.maximumDepth()) {
			monitor.throwIfCancelled();

			final Set<Path> unanalyzed = frontier.stream().map(ArchiveDigPlan.Region::path)
					.filter(path -> !analyzedListings.containsKey(path))
					.collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
			if (analyzedListings.size() + unanalyzed.size() > planning.maximumAnalyzedDirectories()
					|| planningTimeExceeded(started)) {
				break;
			}

			final List<ArchiveDigPlan.Region> next = new ArrayList<>();
			boolean expanded = false;
			for (final ArchiveDigPlan.Region region : frontier) {
				monitor.throwIfCancelled();
				List<Path> listing = analyzedListings.get(region.path());
				if (listing == null) {
					listing = list(region.path());
					analyzedListings.put(region.path(), listing);
				}
				final List<Path> directories = listing.stream().filter(Files::isDirectory).toList();
				if (directories.isEmpty()) {
					next.add(region);
				} else {
					expanded = true;
					directories.stream().map(path -> new ArchiveDigPlan.Region(region.root(), path)).forEach(next::add);
				}
			}

			depth++;
			frontier = next;
			reportPlanning(monitor, depth, frontier.size(), false);
			if (!expanded) {
				break;
			}
		}

		reportPlanning(monitor, depth, frontier.size(), true);
		return new ArchiveDigPlan(analyzedListings, frontier, depth);
	}

	private boolean planningTimeExceeded(final long started) {
		final Duration elapsed = Duration.ofNanos(System.nanoTime() - started);
		return elapsed.compareTo(planning.maximumDuration()) >= 0;
	}

	private static void reportPlanning(final Monitor monitor, final int depth, final int regions,
			final boolean complete) {
		final String message = complete
				? "Crawl planning complete at depth " + depth + ": " + regions + " approximate archive regions."
				: "Planning crawl depth " + depth + ": " + regions + " candidate archive regions.";
		monitor.report(CrawlProgress.indeterminate(CrawlProgress.Phase.PLANNING, message));
	}

	private static List<Path> list(final Path path) throws IOException {
		/*
		 * Need a try-with to close the stream or else the JVM will sooner or later
		 * crash with a java.io.IOException: Too many open files.
		 */
		try (Stream<Path> files = Files.list(path)) {
			return files.sorted(Comparator.comparing(Path::toString)).toList();
		}
	}

	ArchiveNode dig(final Path root, final ArchiveDigPlan plan, final Monitor monitor) throws IOException {
		if (!Files.isDirectory(root)) {
			throw new IllegalArgumentException("Expected a folder but got: " + root);
		}
		return dig(root, root, plan, monitor, false);
	}

	private Set<Clue> createSyntheticClues(final Path root, final Path path) {
		final Set<Clue> clues = new HashSet<>();

		// The artificial id based on a hash of the relative path
		final String archiveId = descriptor.getId().get();
		final String relative = root.relativize(path).toString().replace('\\', '/');
		final String basis = archiveId + "::" + relative;
		final byte[] hash = Hashes.sha256(basis);
		// 16 bytes -> 32 hex chars. Usually plenty, much smaller than full paths.
		final String id = Hashes.toHex(hash, 16);
		clues.add(Clue.internal(Clue.KEY_INTERNAL_ID, id));

		// The folder name without parent folder path
		final String folder = path.getFileName().toString();
		clues.add(Clue.internal(Clue.KEY_INTERNAL_FOLDER, folder));

		return clues;
	}

	/**
	 * @param root    Guaranteed to be a folder (not a file)
	 * @param path    Guaranteed to be a folder (not a file)
	 * @param monitor
	 * @return
	 * @throws IOException
	 */
	private ArchiveNode dig(final Path root, final Path path, final ArchiveDigPlan plan, final Monitor monitor,
			final boolean parentInsideRegion) throws IOException {
		final String pathName;
		if (path.equals(root)) {
			/**
			 * In order to get unique root node ids among various buckets we need to use the
			 * folder's name including all parents (the full path) for the root node. All
			 * other nodes below it just use the folder name itself.
			 */
			pathName = path.toString();
		} else {
			pathName = path.getFileName().toString();
		}
		monitor.throwIfCancelled();
		final boolean startsRegion = plan.isRegionRoot(root, path);
		final boolean insideRegion = parentInsideRegion || startsRegion;
		plan.reportCurrent(path, insideRegion, monitor);

		final List<Path> paths = plan.listing(path).orElse(null);
		final List<Path> effectivePaths = paths == null ? list(path) : paths;

		final ArchivePath node = new ArchivePath(path, effectivePaths);
		final Set<Clue> clues = clueFinder.find(node, monitor);
		monitor.throwIfCancelled();

		final Artifact artifact;
		if (clues.isEmpty()) {
			artifact = null;
		} else {
			final Set<Clue> effectiveClues = new HashSet<>(clues);
			final Set<Clue> syntheticClues = createSyntheticClues(root, path);
			effectiveClues.addAll(syntheticClues);
			artifact = new Artifact(effectiveClues);
			logger.info("Found artifact at: " + root.relativize(path).toString());
		}

		final List<ArchiveNode> children = new ArrayList<>();
		for (final Path child : effectivePaths) {
			if (Files.isDirectory(child)) {
				children.add(dig(root, child, plan, monitor, insideRegion));
			}
		}

		final List<ArchiveNode> effectiveChildren = children.isEmpty() ? null : children;
		final ArchiveNode result = new ArchiveNode(pathName, artifact, effectiveChildren);
		if (startsRegion) {
			plan.completeRegion(path, monitor);
		}
		return result;
	}
}
