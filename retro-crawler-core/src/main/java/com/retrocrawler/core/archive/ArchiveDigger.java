package com.retrocrawler.core.archive;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.retrocrawler.core.CrawlException;
import com.retrocrawler.core.CrawlProgressStages;
import com.retrocrawler.core.Journal;
import com.retrocrawler.core.archive.clues.ArchiveFileView;
import com.retrocrawler.core.archive.clues.ArchiveFolderView;
import com.retrocrawler.core.archive.clues.ArchiveNode;
import com.retrocrawler.core.archive.clues.Artifact;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.ClueAccumulator;
import com.retrocrawler.core.archive.clues.ClueFileIOException;
import com.retrocrawler.core.archive.clues.ClueFinder;
import com.retrocrawler.core.archive.clues.ClueFindingException;
import com.retrocrawler.core.archive.clues.ClueSource;
import com.retrocrawler.core.archive.clues.Clues;
import com.retrocrawler.core.archive.clues.DuplicateClueException;
import com.retrocrawler.core.archive.clues.InternalClueKeys;
import com.retrocrawler.core.archive.filter.ArchivePathFilter;
import com.retrocrawler.core.archive.source.ArchiveEntry;
import com.retrocrawler.core.archive.source.ArchiveFile;
import com.retrocrawler.core.archive.source.ArchiveFolder;
import com.retrocrawler.core.archive.source.ArchiveListing;
import com.retrocrawler.core.archive.source.ArchiveSession;
import com.retrocrawler.core.archive.source.ArchiveSource;
import com.retrocrawler.core.archive.source.FileSystemArchiveSource;
import com.retrocrawler.core.util.Hashes;

public class ArchiveDigger {

	private static final Logger logger = LoggerFactory.getLogger(ArchiveDigger.class);

	private final ArchiveDescriptor descriptor;

	private final List<ClueFinder> clueFinders;

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
		this.clueFinders = List.copyOf(Objects.requireNonNull(archive.clueFinders(), "archive.clueFinders()"));
		if (clueFinders.isEmpty()) {
			throw new IllegalArgumentException("Require at least one clue finder.");
		}
		this.source = Objects.requireNonNull(source, "source");
		this.planning = Objects.requireNonNull(planning, "planning");
		this.pathFilters = List.copyOf(Objects.requireNonNull(archive.pathFilters(), "archive.pathFilters()"));
	}

	public ArchiveNode dig(final Path path, final Journal journal) throws IOException {
		Objects.requireNonNull(journal, "journal");
		final int failuresBeforeCrawling = journal.failureCount();
		final Instant crawledAt = Instant.now();
		try (ArchiveSession session = open(path)) {
			final ArchiveDigTarget target = rootTarget(session);
			final ArchiveDigPlan plan = plan(List.of(target), journal);
			final ArchiveNode result = dig(target, plan, crawledAt, journal);
			final List<Exception> failures = journal.failures();
			if (failures.size() > failuresBeforeCrawling) {
				throw new CrawlException(failures.subList(failuresBeforeCrawling, failures.size()));
			}
			return result;
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

	Optional<ArchiveDigTarget> target(final ArchiveSession session, final Path path, final Journal journal)
			throws IOException {
		Objects.requireNonNull(session, "session");
		Objects.requireNonNull(path, "path");
		Objects.requireNonNull(journal, "journal");
		final ArchiveFolder root = Objects.requireNonNull(session.root(), "session.root()");
		final Path normalizedRoot = root.path().normalize();
		final Path normalizedPath = path.normalize();
		if (!normalizedPath.startsWith(normalizedRoot)) {
			throw new IllegalArgumentException(
					"Expected archive path '" + path + "' to be below root '" + root.path() + "'.");
		}
		if (normalizedPath.equals(normalizedRoot)) {
			return Optional.of(new ArchiveDigTarget(session, root, root));
		}

		ArchiveFolder current = root;
		for (final Path folderName : normalizedRoot.relativize(normalizedPath)) {
			journal.throwIfCancelled();
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

	ArchiveDigPlan plan(final Collection<ArchiveDigTarget> targets, final Journal journal) throws IOException {
		Objects.requireNonNull(targets, "targets");
		Objects.requireNonNull(journal, "journal");
		journal.throwIfCancelled();

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
		reportPlanning(journal, depth, frontier.size(), false);

		while (frontier.size() < planning.targetRegions() && depth < planning.maximumDepth()) {
			journal.throwIfCancelled();

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
				journal.throwIfCancelled();
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
			reportPlanning(journal, depth, frontier.size(), false);
			if (!expanded) {
				break;
			}
		}

		reportPlanning(journal, depth, frontier.size(), true);
		return new ArchiveDigPlan(analyzedListings, frontier, depth);
	}

	private boolean planningTimeExceeded(final long started) {
		final Duration elapsed = Duration.ofNanos(System.nanoTime() - started);
		return elapsed.compareTo(planning.maximumDuration()) >= 0;
	}

	private static void reportPlanning(final Journal journal, final int depth, final int regions,
			final boolean complete) {
		final String message = complete
				? "Crawl planning complete at depth " + depth + ": " + regions + " approximate archive regions."
				: "Planning crawl depth " + depth + ": " + regions + " candidate archive regions.";
		journal.indeterminate(CrawlProgressStages.PLANNING, message);
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

	ArchiveNode dig(final ArchiveDigTarget target, final ArchiveDigPlan plan, final Journal journal)
			throws IOException {
		return dig(target, plan, Instant.now(), journal);
	}

	ArchiveNode dig(final ArchiveDigTarget target, final ArchiveDigPlan plan, final Instant crawledAt,
			final Journal journal) throws IOException {
		Objects.requireNonNull(target, "target");
		Objects.requireNonNull(crawledAt, "crawledAt");
		Objects.requireNonNull(journal, "journal");
		if (!target.folder().path().normalize().startsWith(target.root().path().normalize())) {
			throw new IllegalArgumentException("Expected archive path '" + target.folder().path()
					+ "' to be below root '" + target.root().path() + "'.");
		}
		return digFolder(target.session(), target.root(), target.folder(), plan, crawledAt, journal, false).node();
	}

	private Clues createSyntheticClues(final ArchiveFolder root, final ArchiveFolder folder) {
		final String archiveId = descriptor.id().value();
		final String relative = root.path().relativize(folder.path()).toString().replace('\\', '/');
		final String basis = archiveId + "::" + relative;
		final byte[] hash = Hashes.sha256(basis);
		final String id = Hashes.toHex(hash, 16);

		return Clues.of(Clue.internal(InternalClueKeys.ID, id), Clue.internal(InternalClueKeys.FOLDER, folder.name()));
	}

	private Clues findClues(final ArchiveFolderView folder, final Journal journal,
			final Consumer<ClueFindingException> failures) {
		final ClueAccumulator clues = Clues.accumulator();
		for (final ClueFinder finder : clueFinders) {
			journal.throwIfCancelled();
			final FinderObservation observation = new FinderObservation(finder);
			try {
				final Clues found = Objects.requireNonNull(finder.find(observation.observe(folder)),
						"finder.find(folder)");
				clues.observing(observation.source()).addAll(found);
			} catch (final com.retrocrawler.core.progress.ProgressCancelledException cancelled) {
				throw cancelled;
			} catch (final RuntimeException failure) {
				failures.accept(ClueFindingException.from(observation.source(), failure));
			}
		}
		return clues.clues();
	}

	private DigResult digFolder(final ArchiveSession session, final ArchiveFolder root, final ArchiveFolder folder,
			final ArchiveDigPlan plan, final Instant crawledAt, final Journal journal, final boolean parentInsideRegion)
			throws IOException {
		final String pathName = folder.path().equals(root.path()) ? "." : folder.name();
		journal.throwIfCancelled();
		final boolean startsRegion = plan.isRegionRoot(session, folder);
		final boolean insideRegion = parentInsideRegion || startsRegion;
		plan.reportCurrent(folder, insideRegion, journal);

		FolderListing found = plan.listing(session, folder).orElse(null);
		if (found == null) {
			found = list(session, folder);
		}
		final FolderListing listing = found;

		final List<DigResult> children = new ArrayList<>();
		for (final ArchiveFolder child : listing.folders()) {
			children.add(digFolder(session, root, child, plan, crawledAt, journal, insideRegion));
		}

		final Path relativeFolder = root.path().relativize(folder.path());
		final ArchiveFolderView folderView = folderView(session, folder, listing.files(), children, journal);
		final int failuresBeforeFinding = journal.failureCount();
		final Clues clues = findClues(folderView, journal,
				failure -> journal.record(failure.in(descriptor.id(), relativeFolder)));
		final boolean failed = journal.failureCount() > failuresBeforeFinding;
		journal.throwIfCancelled();

		Artifact artifact = null;
		FolderOutcome outcome;
		if (failed) {
			outcome = new FolderOutcome.Failed();
		} else if (clues.isEmpty()) {
			outcome = new FolderOutcome.MetadataFolder(folderView);
		} else {
			try {
				artifact = new Artifact(clues.and(createSyntheticClues(root, folder)));
				/*
				 * The view is deliberately not carried. This folder is another
				 * item's evidence, so there must be nothing here for an
				 * ancestor to read.
				 */
				outcome = new FolderOutcome.EstablishedArtifact();
				logger.info("Found artifact at: " + relativeFolder);
			} catch (final DuplicateClueException duplicate) {
				// A synthetic clue collided; no finder was reading anything.
				journal.record(new ClueFindingException(duplicate.getMessage(), null, duplicate).in(descriptor.id(),
						relativeFolder));
				outcome = new FolderOutcome.Failed();
			}
		}

		final List<ArchiveNode> archiveChildren = children.stream().map(DigResult::node).toList();
		final List<ArchiveNode> effectiveChildren = archiveChildren.isEmpty() ? null : archiveChildren;
		final ArchiveNode result = new ArchiveNode(pathName, crawledAt, artifact, effectiveChildren);
		if (startsRegion) {
			plan.completeRegion(folder, journal);
		}
		return new DigResult(result, outcome);
	}

	private static ArchiveFolderView folderView(final ArchiveSession session, final ArchiveFolder folder,
			final List<ArchiveFile> files, final List<DigResult> children, final Journal journal) {
		/*
		 * Children are pruned deliberately, not as an optimization. The archive
		 * tree expresses gear containment, never gear type, so a finder may
		 * descend through non-gear subfolders belonging to one item but must
		 * never reach into another piece of gear and absorb its identity.
		 * Removing this filter would let a parent be classified by what its
		 * children are.
		 *
		 * Pruning is structural rather than a check anyone has to remember: a
		 * child that established an artifact carries no view, so there is
		 * nothing here to take. An outcome that established nothing carries
		 * none either, and this switch stops compiling until it says so.
		 */
		final List<ArchiveFolderView> metadataFolders = children.stream().map(DigResult::outcome)
				.flatMap(outcome -> switch (outcome) {
				case FolderOutcome.MetadataFolder metadata -> Stream.of(metadata.view());
				case FolderOutcome.EstablishedArtifact ignored -> Stream.<ArchiveFolderView> empty();
				case FolderOutcome.Failed ignored -> Stream.<ArchiveFolderView> empty();
				}).toList();
		final List<ArchiveFileView> fileViews = files.stream()
				.map(file -> new DefaultArchiveFileView(session, file, journal)).map(ArchiveFileView.class::cast)
				.toList();
		return new DefaultArchiveFolderView(folder.name(), metadataFolders, fileViews);
	}

	/**
	 * What the crawl established about one folder, and with it whatever an
	 * ancestor's finder is allowed to read from that folder.
	 * <p>
	 * Metadata status is established, never inferred, and the readable view
	 * travels with the finding rather than beside it. A folder that established
	 * an artifact has no view to hand up, so an ancestor cannot reach into
	 * another item's evidence even by mistake — the artifact boundary that
	 * design principle 10 depends on is structural instead of a filter someone
	 * has to remember to apply.
	 * <p>
	 * A failed folder likewise establishes nothing and carries no view. Because
	 * this type is sealed, every exhaustive switch has to handle that state
	 * explicitly.
	 */
	private sealed interface FolderOutcome {

		/**
		 * Positively read, no clue found: the folder holds no item of its own,
		 * so an ancestor's finder may read through it.
		 */
		record MetadataFolder(ArchiveFolderView view) implements FolderOutcome {
		}

		/**
		 * Established an artifact, so its clues are another item's evidence.
		 * Whether that evidence ever resolves into gear is a later,
		 * model-dependent question the digger neither knows nor needs.
		 */
		record EstablishedArtifact() implements FolderOutcome {
		}

		/**
		 * Clue finding failed, so no conclusion or readable view may escape.
		 */
		record Failed() implements FolderOutcome {
		}
	}

	private record DigResult(ArchiveNode node, FolderOutcome outcome) {
	}

	private record DefaultArchiveFolderView(String name, List<ArchiveFolderView> folders, List<ArchiveFileView> files)
			implements ArchiveFolderView {

		private DefaultArchiveFolderView {
			Objects.requireNonNull(name, "name");
			folders = List.copyOf(Objects.requireNonNull(folders, "folders"));
			files = List.copyOf(Objects.requireNonNull(files, "files"));
		}
	}

	private record DefaultArchiveFileView(ArchiveSession session, ArchiveFile file, Journal journal)
			implements ArchiveFileView {

		private DefaultArchiveFileView {
			Objects.requireNonNull(session, "session");
			Objects.requireNonNull(file, "file");
			Objects.requireNonNull(journal, "journal");
		}

		@Override
		public String name() {
			return file.name();
		}

		@Override
		public <T> Optional<T> peek(final Function<? super InputStream, ? extends T> inspector) {
			Objects.requireNonNull(inspector, "inspector");
			journal.throwIfCancelled();
			try {
				final Optional<T> result = session.access(file, inspector::apply);
				journal.throwIfCancelled();
				return result;
			} catch (final IOException e) {
				throw new ClueFileIOException("Could not inspect clue file at: " + file.path(), e);
			}
		}
	}

	/** Tracks the resources one unified finder actually inspects. */
	private static final class FinderObservation {

		private final ClueFinder finder;

		private final Set<String> inspectedFiles = new LinkedHashSet<>();

		private FinderObservation(final ClueFinder finder) {
			this.finder = Objects.requireNonNull(finder, "finder");
		}

		private ArchiveFolderView observe(final ArchiveFolderView folder) {
			return new ObservedArchiveFolderView(folder, this, "");
		}

		private ClueSource source() {
			return inspectedFiles.size() == 1 ? ClueSource.fileContent(inspectedFiles.iterator().next(), finder)
					: ClueSource.folderView(finder);
		}
	}

	private record ObservedArchiveFolderView(ArchiveFolderView delegate, FinderObservation observation,
			String relativePath) implements ArchiveFolderView {

		@Override
		public String name() {
			return delegate.name();
		}

		@Override
		public List<ArchiveFolderView> folders() {
			return delegate.folders().stream().map(
					folder -> new ObservedArchiveFolderView(folder, observation, child(relativePath, folder.name())))
					.map(ArchiveFolderView.class::cast).toList();
		}

		@Override
		public List<ArchiveFileView> files() {
			return delegate.files().stream()
					.map(file -> new ObservedArchiveFileView(file, observation, child(relativePath, file.name())))
					.map(ArchiveFileView.class::cast).toList();
		}

		private static String child(final String parent, final String name) {
			return parent.isEmpty() ? name : parent + "/" + name;
		}
	}

	private record ObservedArchiveFileView(ArchiveFileView delegate, FinderObservation observation, String relativePath)
			implements ArchiveFileView {

		@Override
		public String name() {
			return delegate.name();
		}

		@Override
		public <T> Optional<T> peek(final Function<? super InputStream, ? extends T> inspector) {
			observation.inspectedFiles.add(relativePath);
			return delegate.peek(inspector);
		}
	}
}
