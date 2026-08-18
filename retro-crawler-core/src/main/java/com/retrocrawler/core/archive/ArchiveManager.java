package com.retrocrawler.core.archive;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.retrocrawler.core.CrawlException;
import com.retrocrawler.core.CrawlProgressStages;
import com.retrocrawler.core.Journal;
import com.retrocrawler.core.archive.clues.Archive;
import com.retrocrawler.core.archive.clues.ArchiveNode;
import com.retrocrawler.core.archive.clues.ArchiveVersion;
import com.retrocrawler.core.archive.source.ArchiveSession;
import com.retrocrawler.core.progress.Progressor;

public class ArchiveManager {

	private static final Logger logger = LoggerFactory.getLogger(ArchiveManager.class);

	private Archive cache;

	private final ArchiveDescriptor descriptor;

	private final Repository repository;

	private final ArchiveDigger digger;

	private final Clock clock;

	public ArchiveManager(final ArchiveDescriptor descriptor, final ArchiveDigger digger, final Repository repository) {
		this(descriptor, digger, repository, Clock.systemUTC());
	}

	ArchiveManager(final ArchiveDescriptor descriptor, final ArchiveDigger digger, final Repository repository,
			final Clock clock) {
		this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
		this.digger = Objects.requireNonNull(digger, "digger");
		this.repository = Objects.requireNonNull(repository, "repository");
		this.clock = Objects.requireNonNull(clock, "clock");
	}

	private Archive fromSource(final Journal journal) throws IOException {
		final Progressor progressor = journal.progressor();
		final int failuresBeforeCrawling = journal.failureCount();
		final Path root = descriptor.root();
		final Instant crawledAt = clock.instant();
		final ArchiveNode rootNode;
		try (OpenedRoot opened = new OpenedRoot(root)) {
			final ArchiveDigPlan plan = digger.plan(List.of(opened.target()), progressor);
			progressor.throwIfCancelled();
			rootNode = digger.dig(opened.target(), plan, crawledAt, journal);
		}
		requireNoNewFailures(journal, failuresBeforeCrawling);
		final Archive archive = Archive.of(descriptor.id(), root, rootNode);
		progressor.throwIfCancelled();
		progressor.indeterminate(CrawlProgressStages.STOWING, "Stowing away the extracted clue archive.");
		repository.stowaway(archive);
		return archive;
	}

	private Archive fromSubtrees(final Journal journal, final Collection<ARI> requestedSubtrees) throws IOException {
		final Progressor progressor = journal.progressor();
		final int failuresBeforeCrawling = journal.failureCount();
		final Archive stored = retrieveRequiredArchive();
		final List<LocatedSubtree> located = locateSubtrees(stored, requestedSubtrees);
		final Instant crawledAt = clock.instant();
		ArchiveNode root = stored.root();
		try (OpenedRoot opened = new OpenedRoot(descriptor.root())) {
			final List<ArchiveDigTarget> targets = new ArrayList<>();
			for (final LocatedSubtree subtree : located) {
				final ArchiveDigTarget target = digger
						.target(opened.target().session(), subtree.requestedPath(), progressor)
						.orElseThrow(() -> new IllegalArgumentException(
								"Archive subtree is not an existing folder; re-index its existing parent instead: "
										+ subtree.requestedPath()));
				targets.add(target);
			}
			final ArchiveDigPlan plan = digger.plan(targets, progressor);

			for (int index = 0; index < located.size(); index++) {
				progressor.throwIfCancelled();
				final ArchiveNode freshNode = digger.dig(targets.get(index), plan, crawledAt, journal);
				root = replace(root, located.get(index).relativeFolders(), freshNode);
			}
		}
		requireNoNewFailures(journal, failuresBeforeCrawling);

		final Archive archive = Archive.of(stored.id(), Path.of(stored.basePath()), root);
		progressor.throwIfCancelled();
		progressor.indeterminate(CrawlProgressStages.STOWING, "Stowing away the partially rebuilt clue archive.");
		repository.stowaway(archive);
		return archive;
	}

	private static void requireNoNewFailures(final Journal journal, final int previousFailureCount) {
		final List<Exception> failures = journal.failures();
		if (failures.size() > previousFailureCount) {
			throw new CrawlException(failures.subList(previousFailureCount, failures.size()));
		}
	}

	public synchronized Archive archive(final Journal journal, final ReindexScope reindexScope) throws IOException {
		Objects.requireNonNull(journal, "journal");
		Objects.requireNonNull(reindexScope, "reindexScope");

		if (reindexScope.kind() == ReindexScope.Kind.NONE) {
			if (cache != null) {
				return cache;
			}
			cache = retrieve().orElse(null);
			if (cache != null) {
				return cache;
			}
		}
		cache = switch (reindexScope.kind()) {
		case NONE, ALL -> fromSource(journal);
		case SUBTREES -> fromSubtrees(journal, reindexScope.subtrees());
		};
		return cache;
	}

	private Archive retrieveRequiredArchive() {
		if (cache != null) {
			return cache;
		}
		final Optional<Archive> stored = repository.retrieve(descriptor.id());
		if (stored.isEmpty()) {
			throw new IllegalStateException(
					"Cannot re-index archive subtrees because no stored clue archive exists. Re-index the complete archive first.");
		}
		return bindToConfiguredRoot(stored.get());
	}

	private Optional<Archive> retrieve() {
		try {
			return repository.retrieve(descriptor.id()).map(this::bindToConfiguredRoot);
		} catch (final RepositoryException e) {
			logger.warn("Could not retrieve archive '{}'. The configured archive source will be crawled again.",
					descriptor.id(), e);
			return Optional.empty();
		}
	}

	private Archive bindToConfiguredRoot(final Archive stored) {
		if (!ArchiveVersion.CURRENT_IMPLEMENTATION_VERSION.equals(stored.version())) {
			throw new RepositoryException("Stored archive '" + stored.id() + "' uses cache version " + stored.version()
					+ " but this crawler requires " + ArchiveVersion.CURRENT_IMPLEMENTATION_VERSION + ".");
		}
		final Path configuredRoot = descriptor.root();
		if (normalize(Path.of(stored.basePath())).equals(normalize(configuredRoot))) {
			return stored;
		}
		return Archive.of(stored.id(), configuredRoot, stored.root());
	}

	private List<LocatedSubtree> locateSubtrees(final Archive stored, final Collection<ARI> requestedSubtrees) {
		final List<Path> requestedPaths = Objects.requireNonNull(requestedSubtrees, "requestedSubtrees").stream()
				.map(this::sourcePath).toList();
		final List<Path> effectivePaths = eliminateNestedPaths(requestedPaths);
		final Path configuredRoot = normalize(descriptor.root());
		final List<LocatedSubtree> result = new ArrayList<>();

		for (final Path requestedPath : effectivePaths) {
			if (!requestedPath.startsWith(configuredRoot)) {
				throw new IllegalArgumentException("Archive subtree is not below the configured root of archive '"
						+ descriptor.id() + "': " + requestedPath);
			}
			final List<String> relativeFolders = relativeFolders(configuredRoot, requestedPath);
			requireStoredSubtree(stored.root(), relativeFolders, requestedPath);
			result.add(new LocatedSubtree(requestedPath, relativeFolders));
		}
		return result;
	}

	private Path sourcePath(final ARI subtree) {
		Objects.requireNonNull(subtree, "subtree");
		if (!descriptor.id().equals(subtree.archiveId())) {
			throw new IllegalArgumentException("ARI belongs to archive '" + subtree.archiveId() + "' instead of '"
					+ descriptor.id() + "': " + subtree);
		}
		Path sourcePath = normalize(descriptor.root());
		if (!subtree.resourcePath().toString().isEmpty()) {
			for (final Path segment : subtree.resourcePath()) {
				sourcePath = sourcePath.resolve(segment.toString());
			}
		}
		return sourcePath.normalize();
	}

	private static List<Path> eliminateNestedPaths(final Collection<Path> requestedPaths) {
		final Set<Path> unique = new LinkedHashSet<>();
		for (final Path path : Objects.requireNonNull(requestedPaths, "requestedPaths")) {
			unique.add(normalize(Objects.requireNonNull(path, "path")));
		}
		final List<Path> sorted = unique.stream()
				.sorted(Comparator.comparingInt(Path::getNameCount).thenComparing(Path::toString)).toList();
		final List<Path> result = new ArrayList<>();
		for (final Path candidate : sorted) {
			if (result.stream().noneMatch(candidate::startsWith)) {
				result.add(candidate);
			}
		}
		return List.copyOf(result);
	}

	private static Path normalize(final Path path) {
		return path.normalize();
	}

	private static List<String> relativeFolders(final Path root, final Path path) {
		if (root.equals(path)) {
			return List.of();
		}
		final List<String> result = new ArrayList<>();
		root.relativize(path).forEach(folder -> result.add(folder.toString()));
		return List.copyOf(result);
	}

	private static void requireStoredSubtree(final ArchiveNode root, final List<String> relativeFolders,
			final Path requestedPath) {
		ArchiveNode current = root;
		for (final String folder : relativeFolders) {
			current = child(current, folder).orElseThrow(() -> new IllegalArgumentException(
					"Archive subtree is not present in the stored clue archive; re-index its parent instead: "
							+ requestedPath));
		}
	}

	private static ArchiveNode replace(final ArchiveNode current, final List<String> relativeFolders,
			final ArchiveNode replacement) {
		if (relativeFolders.isEmpty()) {
			return replacement;
		}

		final String folder = relativeFolders.getFirst();
		final List<ArchiveNode> children = current.children();
		if (children == null) {
			throw new IllegalStateException("Stored archive tree no longer contains expected folder: " + folder);
		}

		final List<ArchiveNode> replacements = new ArrayList<>(children);
		for (int index = 0; index < replacements.size(); index++) {
			final ArchiveNode candidate = replacements.get(index);
			if (folder.equals(candidate.folder())) {
				final ArchiveNode replaced = replace(candidate, relativeFolders.subList(1, relativeFolders.size()),
						replacement);
				replacements.set(index, replaced);
				return new ArchiveNode(current.folder(), current.crawledAt(), current.artifact(), replacements);
			}
		}
		throw new IllegalStateException("Stored archive tree no longer contains expected folder: " + folder);
	}

	private static Optional<ArchiveNode> child(final ArchiveNode node, final String folder) {
		final List<ArchiveNode> children = node.children();
		if (children == null) {
			return Optional.empty();
		}
		return children.stream().filter(child -> folder.equals(child.folder())).findFirst();
	}

	private final class OpenedRoot implements AutoCloseable {

		private final ArchiveDigTarget target;

		private OpenedRoot(final Path root) throws IOException {
			final ArchiveSession session = digger.open(Objects.requireNonNull(root, "root"));
			try {
				target = digger.rootTarget(session);
			} catch (final IOException | RuntimeException failure) {
				try {
					session.close();
				} catch (final IOException closeFailure) {
					failure.addSuppressed(closeFailure);
				}
				throw failure;
			}
		}

		private ArchiveDigTarget target() {
			return target;
		}

		@Override
		public void close() throws IOException {
			target.session().close();
		}
	}

	private record LocatedSubtree(Path requestedPath, List<String> relativeFolders) {
	}

}
