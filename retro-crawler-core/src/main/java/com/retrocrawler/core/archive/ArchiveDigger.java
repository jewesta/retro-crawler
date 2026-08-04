package com.retrocrawler.core.archive;

import java.io.IOException;
import java.io.InputStream;
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
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.retrocrawler.core.archive.clues.ArchiveFileView;
import com.retrocrawler.core.archive.clues.ArchiveFolderView;
import com.retrocrawler.core.archive.clues.ArchiveNode;
import com.retrocrawler.core.archive.clues.ArchivePathClueFinder;
import com.retrocrawler.core.archive.clues.Artifact;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.ClueFileIOException;
import com.retrocrawler.core.archive.clues.InternalClueKeys;
import com.retrocrawler.core.archive.filter.ArchivePathFilter;
import com.retrocrawler.core.archive.source.ArchiveEntry;
import com.retrocrawler.core.archive.source.ArchiveFile;
import com.retrocrawler.core.archive.source.ArchiveFolder;
import com.retrocrawler.core.archive.source.ArchiveListing;
import com.retrocrawler.core.archive.source.ArchiveSession;
import com.retrocrawler.core.archive.source.ArchiveSource;
import com.retrocrawler.core.archive.source.FileSystemArchiveSource;
import com.retrocrawler.core.progress.ProgressStage;
import com.retrocrawler.core.progress.Progressor;
import com.retrocrawler.core.util.Hashes;

public class ArchiveDigger {

	private static final Logger logger = LoggerFactory.getLogger(ArchiveDigger.class);

	private final ArchiveDescriptor descriptor;

	private final ArchivePathClueFinder clueFinder;

	private final CrawlPlanning planning;

	private final List<ArchivePathFilter> pathFilters;

	private final ArchiveSource source;

	public ArchiveDigger(final ArchiveDefinition archive) {
		this(archive, new FileSystemArchiveSource(), CrawlPlanning.defaults());
	}

	public ArchiveDigger(final ArchiveDefinition archive, final CrawlPlanning planning) {
		this(archive, new FileSystemArchiveSource(), planning);
	}

	public ArchiveDigger(final ArchiveDefinition archive, final ArchiveSource source) {
		this(archive, source, CrawlPlanning.defaults());
	}

	public ArchiveDigger(final ArchiveDefinition archive, final ArchiveSource source, final CrawlPlanning planning) {
		Objects.requireNonNull(archive, "archive");
		this.descriptor = Objects.requireNonNull(archive.archiveDescriptor(), "archive.archiveDescriptor()");
		this.clueFinder = Objects.requireNonNull(archive.archivePathClueFinder(), "archive.archivePathClueFinder()");
		this.source = Objects.requireNonNull(source, "source");
		this.planning = Objects.requireNonNull(planning, "planning");
		this.pathFilters = List.copyOf(Objects.requireNonNull(archive.pathFilters(), "archive.pathFilters()"));
	}

	public ArchiveNode dig(final Path path, final Progressor progressor) throws IOException {
		try (ArchiveSession session = open(path)) {
			final ArchiveDigTarget target = rootTarget(session);
			final ArchiveDigPlan plan = plan(List.of(target), progressor);
			return dig(target, plan, progressor);
		}
	}

	ArchiveSession open(final Path root) throws IOException {
		return source.open(Objects.requireNonNull(root, "root"));
	}

	ArchiveDigTarget rootTarget(final ArchiveSession session) throws IOException {
		Objects.requireNonNull(session, "session");
		final ArchiveFolder root = Objects.requireNonNull(session.root(), "session.root()");
		return new ArchiveDigTarget(session, root, root);
	}

	Optional<ArchiveDigTarget> target(final ArchiveSession session, final Path path, final Progressor progressor)
			throws IOException {
		Objects.requireNonNull(session, "session");
		Objects.requireNonNull(path, "path");
		Objects.requireNonNull(progressor, "progressor");
		final ArchiveFolder root = Objects.requireNonNull(session.root(), "session.root()");
		final Path normalizedRoot = root.path().normalize();
		final Path normalizedPath = path.normalize();
		if (!normalizedPath.startsWith(normalizedRoot)) {
			throw new IllegalArgumentException(
					"Expected archive path '" + path + "' to be below root '" + root.path() + "'.");
		}

		ArchiveFolder current = root;
		for (final Path folderName : normalizedRoot.relativize(normalizedPath)) {
			progressor.throwIfCancelled();
			final Path expected = current.path().resolve(folderName).normalize();
			final Optional<ArchiveFolder> child = sourceListing(session, current).folders().stream()
					.filter(candidate -> candidate.path().normalize().equals(expected)).findFirst();
			if (child.isEmpty()) {
				return Optional.empty();
			}
			current = child.get();
		}
		return Optional.of(new ArchiveDigTarget(session, root, current));
	}

	ArchiveDigPlan plan(final Collection<ArchiveDigTarget> targets, final Progressor progressor) throws IOException {
		Objects.requireNonNull(targets, "targets");
		Objects.requireNonNull(progressor, "progressor");
		progressor.throwIfCancelled();

		final List<ArchiveDigPlan.Region> initialRegions = new ArrayList<>();
		for (final ArchiveDigTarget target : targets) {
			initialRegions.add(new ArchiveDigPlan.Region(target.session(), target.root(), target.folder()));
		}
		if (initialRegions.isEmpty()) {
			throw new IllegalArgumentException("At least one archive dig target is required.");
		}

		final long started = System.nanoTime();
		final Map<ArchiveDigPlan.FolderKey, FolderListing> analyzedListings = new LinkedHashMap<>();
		List<ArchiveDigPlan.Region> frontier = initialRegions;
		int depth = 0;
		reportPlanning(progressor, depth, frontier.size(), false);

		while (frontier.size() < planning.targetRegions() && depth < planning.maximumDepth()) {
			progressor.throwIfCancelled();

			final Set<ArchiveDigPlan.FolderKey> unanalyzed = frontier.stream().map(ArchiveDigPlan.Region::key)
					.filter(key -> !analyzedListings.containsKey(key))
					.collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
			if (analyzedListings.size() + unanalyzed.size() > planning.maximumAnalyzedDirectories()
					|| planningTimeExceeded(started)) {
				break;
			}

			final List<ArchiveDigPlan.Region> next = new ArrayList<>();
			boolean expanded = false;
			for (final ArchiveDigPlan.Region region : frontier) {
				progressor.throwIfCancelled();
				FolderListing listing = analyzedListings.get(region.key());
				if (listing == null) {
					listing = list(region.session(), region.folder());
					analyzedListings.put(region.key(), listing);
				}
				final List<ArchiveFolder> directories = listing.folders();
				if (directories.isEmpty()) {
					next.add(region);
				} else {
					expanded = true;
					directories.stream()
							.map(folder -> new ArchiveDigPlan.Region(region.session(), region.root(), folder))
							.forEach(next::add);
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

	private FolderListing list(final ArchiveSession session, final ArchiveFolder folder) throws IOException {
		final ArchiveListing listing = sourceListing(session, folder);
		final List<ArchiveFolder> folders = listing.folders().stream().filter(this::accept)
				.sorted(Comparator.comparing(entry -> entry.path().toString())).toList();
		final List<ArchiveFile> files = listing.files().stream().filter(this::accept)
				.sorted(Comparator.comparing(entry -> entry.path().toString())).toList();
		return new FolderListing(folders, files);
	}

	private static ArchiveListing sourceListing(final ArchiveSession session, final ArchiveFolder folder)
			throws IOException {
		final ArchiveListing listing = Objects.requireNonNull(session.list(folder), "session.list(folder)");
		final Set<Path> paths = new HashSet<>();
		for (final ArchiveEntry entry : java.util.stream.Stream
				.concat(listing.folders().stream(), listing.files().stream()).toList()) {
			final Path entryPath = Objects.requireNonNull(entry, "archive listing entry").path().normalize();
			if (!Objects.equals(folder.path().normalize(), entryPath.getParent())) {
				throw new IllegalArgumentException("Archive source returned entry '" + entry.path()
						+ "' outside the direct listing of folder '" + folder.path() + "'.");
			}
			if (!paths.add(entryPath)) {
				throw new IllegalArgumentException(
						"Archive source returned a duplicate direct entry at: " + entry.path());
			}
		}
		return listing;
	}

	private boolean accept(final ArchiveEntry entry) {
		return pathFilters.stream().allMatch(filter -> filter.accept(entry.path()));
	}

	ArchiveNode dig(final ArchiveDigTarget target, final ArchiveDigPlan plan, final Progressor progressor)
			throws IOException {
		Objects.requireNonNull(target, "target");
		if (!target.folder().path().normalize().startsWith(target.root().path().normalize())) {
			throw new IllegalArgumentException("Expected archive path '" + target.folder().path()
					+ "' to be below root '" + target.root().path() + "'.");
		}
		return digFolder(target.session(), target.root(), target.folder(), plan, progressor, false).node();
	}

	private Set<Clue> createSyntheticClues(final ArchiveFolder root, final ArchiveFolder folder) {
		final Set<Clue> clues = new HashSet<>();

		final String archiveId = descriptor.id().value();
		final String relative = root.path().relativize(folder.path()).toString().replace('\\', '/');
		final String basis = archiveId + "::" + relative;
		final byte[] hash = Hashes.sha256(basis);
		final String id = Hashes.toHex(hash, 16);
		clues.add(Clue.internal(InternalClueKeys.ID, id));
		clues.add(Clue.internal(InternalClueKeys.FOLDER, folder.name()));

		return clues;
	}

	private DigResult digFolder(final ArchiveSession session, final ArchiveFolder root, final ArchiveFolder folder,
			final ArchiveDigPlan plan, final Progressor progressor, final boolean parentInsideRegion)
			throws IOException {
		final String pathName = folder.path().equals(root.path()) ? "." : folder.name();
		progressor.throwIfCancelled();
		final boolean startsRegion = plan.isRegionRoot(session, folder);
		final boolean insideRegion = parentInsideRegion || startsRegion;
		plan.reportCurrent(folder, insideRegion, progressor);

		FolderListing listing = plan.listing(session, folder).orElse(null);
		if (listing == null) {
			listing = list(session, folder);
		}

		final Set<Clue> localClues = clueFinder.find(root, folder, listing.files(), session, progressor);
		progressor.throwIfCancelled();

		final List<DigResult> children = new ArrayList<>();
		for (final ArchiveFolder child : listing.folders()) {
			children.add(digFolder(session, root, child, plan, progressor, insideRegion));
		}

		final ArchiveFolderView folderView = folderView(session, folder, listing.files(), children, progressor);
		final Set<Clue> clues = clueFinder.enrich(localClues, folderView, progressor);
		progressor.throwIfCancelled();

		final Artifact artifact;
		if (clues.isEmpty()) {
			artifact = null;
		} else {
			final Set<Clue> effectiveClues = new HashSet<>(clues);
			effectiveClues.addAll(createSyntheticClues(root, folder));
			artifact = new Artifact(effectiveClues);
			logger.info("Found artifact at: " + root.path().relativize(folder.path()));
		}

		final List<ArchiveNode> archiveChildren = children.stream().map(DigResult::node).toList();
		final List<ArchiveNode> effectiveChildren = archiveChildren.isEmpty() ? null : archiveChildren;
		final ArchiveNode result = new ArchiveNode(pathName, artifact, effectiveChildren);
		if (startsRegion) {
			plan.completeRegion(folder, progressor);
		}
		return new DigResult(result, folderView);
	}

	private static ArchiveFolderView folderView(final ArchiveSession session, final ArchiveFolder folder,
			final List<ArchiveFile> files, final List<DigResult> children, final Progressor progressor) {
		final List<ArchiveFolderView> metadataFolders = children.stream()
				.filter(child -> child.node().artifact() == null).map(DigResult::folderView).toList();
		final List<ArchiveFileView> fileViews = files.stream()
				.map(file -> new DefaultArchiveFileView(session, file, progressor)).map(ArchiveFileView.class::cast)
				.toList();
		return new DefaultArchiveFolderView(folder.name(), metadataFolders, fileViews);
	}

	private record DigResult(ArchiveNode node, ArchiveFolderView folderView) {
	}

	private record DefaultArchiveFolderView(String name, List<ArchiveFolderView> folders, List<ArchiveFileView> files)
			implements ArchiveFolderView {

		private DefaultArchiveFolderView {
			Objects.requireNonNull(name, "name");
			folders = List.copyOf(Objects.requireNonNull(folders, "folders"));
			files = List.copyOf(Objects.requireNonNull(files, "files"));
		}
	}

	private record DefaultArchiveFileView(ArchiveSession session, ArchiveFile file, Progressor progressor)
			implements ArchiveFileView {

		private DefaultArchiveFileView {
			Objects.requireNonNull(session, "session");
			Objects.requireNonNull(file, "file");
			Objects.requireNonNull(progressor, "progressor");
		}

		@Override
		public String name() {
			return file.name();
		}

		@Override
		public <T> Optional<T> peek(final Function<? super InputStream, ? extends T> inspector) {
			Objects.requireNonNull(inspector, "inspector");
			progressor.throwIfCancelled();
			try {
				final Optional<T> result = session.access(file, inspector::apply);
				progressor.throwIfCancelled();
				return result;
			} catch (final IOException e) {
				throw new ClueFileIOException("Could not inspect clue file at: " + file.path(), e);
			}
		}
	}
}
