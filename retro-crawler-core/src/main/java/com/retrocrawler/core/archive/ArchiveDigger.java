package com.retrocrawler.core.archive;

import java.io.IOException;
import java.io.InputStream;
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
import java.util.function.Function;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.retrocrawler.core.archive.clues.ArchiveNode;
import com.retrocrawler.core.archive.clues.ArchivePathClueFinder;
import com.retrocrawler.core.archive.clues.ArchiveFileView;
import com.retrocrawler.core.archive.clues.ArchiveFolderView;
import com.retrocrawler.core.archive.clues.Artifact;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.ClueFileIOException;
import com.retrocrawler.core.archive.clues.InternalClueKeys;
import com.retrocrawler.core.progress.ProgressStage;
import com.retrocrawler.core.progress.Progressor;
import com.retrocrawler.core.util.Hashes;

public class ArchiveDigger {

	private static final Logger logger = LoggerFactory.getLogger(ArchiveDigger.class);

	private final ArchiveDescriptor descriptor;

	private final ArchivePathClueFinder clueFinder;

	private final CrawlPlanning planning;

	private final CrawlPolicy crawlPolicy;

	public ArchiveDigger(final ArchiveDescriptor descriptor, final ArchivePathClueFinder clueFinder) {
		this(descriptor, clueFinder, CrawlPlanning.defaults(), new CrawlEverything());
	}

	public ArchiveDigger(final ArchiveDescriptor descriptor, final ArchivePathClueFinder clueFinder,
			final CrawlPlanning planning) {
		this(descriptor, clueFinder, planning, new CrawlEverything());
	}

	public ArchiveDigger(final ArchiveDescriptor descriptor, final ArchivePathClueFinder clueFinder,
			final CrawlPlanning planning, final CrawlPolicy crawlPolicy) {
		this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
		this.clueFinder = Objects.requireNonNull(clueFinder, "clueFinder");
		this.planning = Objects.requireNonNull(planning, "planning");
		this.crawlPolicy = Objects.requireNonNull(crawlPolicy, "crawlPolicy");
	}

	public ArchiveNode dig(final Path path, final Progressor progressor) throws IOException {
		final ArchiveDigTarget target = new ArchiveDigTarget(path, path);
		final ArchiveDigPlan plan = plan(List.of(target), progressor);
		return dig(path, plan, progressor);
	}

	ArchiveDigPlan plan(final Collection<ArchiveDigTarget> targets, final Progressor progressor) throws IOException {
		Objects.requireNonNull(targets, "targets");
		Objects.requireNonNull(progressor, "progressor");
		progressor.throwIfCancelled();

		final List<ArchiveDigPlan.Region> initialRegions = new ArrayList<>();
		for (final ArchiveDigTarget target : targets) {
			if (!Files.isDirectory(target.path())) {
				throw new IllegalArgumentException("Expected a folder but got: " + target.path());
			}
			initialRegions.add(new ArchiveDigPlan.Region(target.root(), target.path()));
		}
		if (initialRegions.isEmpty()) {
			throw new IllegalArgumentException("At least one archive dig target is required.");
		}

		final long started = System.nanoTime();
		final Map<Path, FolderListing> analyzedListings = new LinkedHashMap<>();
		List<ArchiveDigPlan.Region> frontier = initialRegions;
		int depth = 0;
		reportPlanning(progressor, depth, frontier.size(), false);

		while (frontier.size() < planning.targetRegions() && depth < planning.maximumDepth()) {
			progressor.throwIfCancelled();

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
				progressor.throwIfCancelled();
				FolderListing listing = analyzedListings.get(region.path());
				if (listing == null) {
					listing = FolderListing.from(list(region.path()));
					analyzedListings.put(region.path(), listing);
				}
				final List<Path> directories = listing.folders();
				if (directories.isEmpty()) {
					next.add(region);
				} else {
					expanded = true;
					directories.stream().map(path -> new ArchiveDigPlan.Region(region.root(), path)).forEach(next::add);
				}
			}

			depth++;
			frontier = next;
			reportPlanning(progressor, depth, frontier.size(), false);
			if (!expanded) {
				break;
			}
		}

		reportPlanning(progressor, depth, frontier.size(), true);
		return new ArchiveDigPlan(analyzedListings, frontier, depth);
	}

	private boolean planningTimeExceeded(final long started) {
		final Duration elapsed = Duration.ofNanos(System.nanoTime() - started);
		return elapsed.compareTo(planning.maximumDuration()) >= 0;
	}

	private static void reportPlanning(final Progressor progressor, final int depth, final int regions,
			final boolean complete) {
		final String message = complete
				? "Crawl planning complete at depth " + depth + ": " + regions + " approximate archive regions."
				: "Planning crawl depth " + depth + ": " + regions + " candidate archive regions.";
		progressor.indeterminate(ProgressStage.PLANNING, message);
	}

	private List<Path> list(final Path path) throws IOException {
		/*
		 * Need a try-with to close the stream or else the JVM will sooner or later
		 * crash with a java.io.IOException: Too many open files.
		 */
		try (Stream<Path> files = Files.list(path)) {
			return files.filter(crawlPolicy::includes).sorted(Comparator.comparing(Path::toString)).toList();
		}
	}

	ArchiveNode dig(final Path root, final ArchiveDigPlan plan, final Progressor progressor) throws IOException {
		return dig(root, root, plan, progressor);
	}

	ArchiveNode dig(final Path root, final Path path, final ArchiveDigPlan plan, final Progressor progressor)
			throws IOException {
		if (!Files.isDirectory(root)) {
			throw new IllegalArgumentException("Expected a folder but got: " + root);
		}
		if (!Files.isDirectory(path)) {
			throw new IllegalArgumentException("Expected a folder but got: " + path);
		}
		if (!path.startsWith(root)) {
			throw new IllegalArgumentException("Expected archive path '" + path + "' to be below root '" + root + "'.");
		}
		return digFolder(root, path, plan, progressor, false).node();
	}

	private Set<Clue> createSyntheticClues(final Path root, final Path path) {
		final Set<Clue> clues = new HashSet<>();

		// The artificial id based on a hash of the relative path
		final String archiveId = descriptor.id().value();
		final String relative = root.relativize(path).toString().replace('\\', '/');
		final String basis = archiveId + "::" + relative;
		final byte[] hash = Hashes.sha256(basis);
		// 16 bytes -> 32 hex chars. Usually plenty, much smaller than full paths.
		final String id = Hashes.toHex(hash, 16);
		clues.add(Clue.internal(InternalClueKeys.ID, id));

		// The folder name without parent folder path
		final String folder = path.getFileName().toString();
		clues.add(Clue.internal(InternalClueKeys.FOLDER, folder));

		return clues;
	}

	/**
	 * @param root    Guaranteed to be a folder (not a file)
	 * @param path    Guaranteed to be a folder (not a file)
	 * @param progressor
	 * @return
	 * @throws IOException
	 */
	private DigResult digFolder(final Path root, final Path path, final ArchiveDigPlan plan,
			final Progressor progressor,
			final boolean parentInsideRegion) throws IOException {
		final String pathName;
		if (path.equals(root)) {
			// A bucket supplies the runtime root; the cached tree begins at archive ".".
			pathName = ".";
		} else {
			pathName = path.getFileName().toString();
		}
		progressor.throwIfCancelled();
		final boolean startsRegion = plan.isRegionRoot(root, path);
		final boolean insideRegion = parentInsideRegion || startsRegion;
		plan.reportCurrent(path, insideRegion, progressor);

		FolderListing listing = plan.listing(path).orElse(null);
		if (listing == null) {
			listing = FolderListing.from(list(path));
		}

		final ArchivePath archivePath = new ArchivePath(root, path, listing.entries());
		final Set<Clue> localClues = clueFinder.find(archivePath, listing.files(), progressor);
		progressor.throwIfCancelled();

		final List<DigResult> children = new ArrayList<>();
		for (final Path child : listing.folders()) {
			children.add(digFolder(root, child, plan, progressor, insideRegion));
		}

		final ArchiveFolderView folderView = folderView(path, listing.files(), children, progressor);
		final Set<Clue> clues = clueFinder.enrich(localClues, folderView, progressor);
		progressor.throwIfCancelled();

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

		final List<ArchiveNode> archiveChildren = children.stream().map(DigResult::node).toList();
		final List<ArchiveNode> effectiveChildren = archiveChildren.isEmpty() ? null : archiveChildren;
		final ArchiveNode result = new ArchiveNode(pathName, artifact, effectiveChildren);
		if (startsRegion) {
			plan.completeRegion(path, progressor);
		}
		return new DigResult(result, folderView);
	}

	private static ArchiveFolderView folderView(final Path path, final List<Path> files,
			final List<DigResult> children, final Progressor progressor) {
		final List<ArchiveFolderView> metadataFolders = children.stream()
				.filter(child -> child.node().artifact() == null)
				.map(DigResult::folderView)
				.toList();
		final List<ArchiveFileView> fileViews = files.stream()
				.map(file -> new DefaultArchiveFileView(file, progressor))
				.map(ArchiveFileView.class::cast)
				.toList();
		return new DefaultArchiveFolderView(folderName(path), metadataFolders, fileViews);
	}

	private static String folderName(final Path path) {
		final Path fileName = path.getFileName();
		return fileName == null ? path.toString() : fileName.toString();
	}

	private record DigResult(ArchiveNode node, ArchiveFolderView folderView) {
	}

	private record DefaultArchiveFolderView(String name, List<ArchiveFolderView> folders,
			List<ArchiveFileView> files) implements ArchiveFolderView {

		private DefaultArchiveFolderView {
			Objects.requireNonNull(name, "name");
			folders = List.copyOf(Objects.requireNonNull(folders, "folders"));
			files = List.copyOf(Objects.requireNonNull(files, "files"));
		}
	}

	private record DefaultArchiveFileView(Path path, Progressor progressor) implements ArchiveFileView {

		private DefaultArchiveFileView {
			Objects.requireNonNull(path, "path");
			Objects.requireNonNull(progressor, "progressor");
		}

		@Override
		public String name() {
			return path.getFileName().toString();
		}

		@Override
		public <T> T peek(final Function<? super InputStream, ? extends T> inspector) {
			Objects.requireNonNull(inspector, "inspector");
			progressor.throwIfCancelled();
			try (InputStream in = Files.newInputStream(path)) {
				final T result = inspector.apply(in);
				progressor.throwIfCancelled();
				return result;
			} catch (final IOException e) {
				throw new ClueFileIOException("Could not inspect clue file at: " + path, e);
			}
		}
	}
}
