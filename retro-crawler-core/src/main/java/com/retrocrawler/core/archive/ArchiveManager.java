package com.retrocrawler.core.archive;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

import com.retrocrawler.core.archive.clues.Archive;
import com.retrocrawler.core.archive.clues.ArchiveNode;
import com.retrocrawler.core.archive.clues.ArchiveVersion;
import com.retrocrawler.core.archive.clues.Bucket;
import com.retrocrawler.core.progress.ProgressStage;
import com.retrocrawler.core.progress.Progressor;

public class ArchiveManager {

	private static final Logger logger = LoggerFactory.getLogger(ArchiveManager.class);

	private Archive cache;

	private final ArchiveDescriptor descriptor;

	private final Repository repository;

	private final ArchiveDigger digger;

	public ArchiveManager(final ArchiveDescriptor descriptor, final ArchiveDigger digger,
			final Repository repository) {
		this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
		this.digger = Objects.requireNonNull(digger, "digger");
		this.repository = Objects.requireNonNull(repository, "repository");
	}

	private Archive fromFileSystem(final Progressor progressor) throws IOException {
		final Collection<Path> rootPaths = descriptor.paths();
		final List<ArchiveDigTarget> targets = rootPaths.stream().map(path -> new ArchiveDigTarget(path, path)).toList();
		final ArchiveDigPlan plan = digger.plan(targets, progressor);
		final List<Bucket> buckets = new ArrayList<>();
		for (final Path rootPath : rootPaths) {
			progressor.throwIfCancelled();
			final ArchiveNode rootNode = digger.dig(rootPath, plan, progressor);
			final Bucket bucket = Bucket.of(rootPath, rootNode);
			buckets.add(bucket);
		}
		final Archive archive = Archive.of(descriptor.id(), buckets);
		progressor.throwIfCancelled();
		progressor.indeterminate(ProgressStage.STOWING, "Stowing away the extracted clue archive.");
		repository.stowaway(archive);
		return archive;
	}

	private Archive fromSubtrees(final Progressor progressor, final Collection<Path> requestedPaths)
			throws IOException {
		final Archive stored = retrieveRequiredArchive();
		final List<LocatedSubtree> subtrees = locateSubtrees(stored, requestedPaths);
		final List<ArchiveDigTarget> targets = subtrees.stream().map(LocatedSubtree::target).toList();
		final ArchiveDigPlan plan = digger.plan(targets, progressor);

		final List<Bucket> buckets = new ArrayList<>(stored.buckets());
		for (final LocatedSubtree subtree : subtrees) {
			progressor.throwIfCancelled();
			final ArchiveDigTarget target = subtree.target();
			final ArchiveNode freshNode = digger.dig(target.root(), target.path(), plan, progressor);
			final Bucket storedBucket = buckets.get(subtree.bucketIndex());
			final ArchiveNode mergedRoot = replace(storedBucket.root(), subtree.relativeFolders(), freshNode);
			buckets.set(subtree.bucketIndex(), Bucket.of(Path.of(storedBucket.basePath()), mergedRoot));
		}

		final Archive archive = Archive.of(stored.id(), buckets);
		progressor.throwIfCancelled();
		progressor.indeterminate(ProgressStage.STOWING, "Stowing away the partially rebuilt clue archive.");
		repository.stowaway(archive);
		return archive;
	}

	public synchronized Archive archive(final Progressor progressor, final ReindexScope reindexScope)
			throws IOException {
		Objects.requireNonNull(progressor, "progressor");
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
		case NONE, ALL -> fromFileSystem(progressor);
		case SUBTREES -> fromSubtrees(progressor, reindexScope.paths());
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
		return bindToConfiguredRoots(stored.get());
	}

	private Optional<Archive> retrieve() {
		try {
			return repository.retrieve(descriptor.id()).map(this::bindToConfiguredRoots);
		} catch (final RepositoryException e) {
			logger.warn("Could not retrieve archive '{}'. The filesystem archive will be crawled again.",
					descriptor.id(), e);
			return Optional.empty();
		}
	}

	private Archive bindToConfiguredRoots(final Archive stored) {
		if (!ArchiveVersion.CURRENT_IMPLEMENTATION_VERSION.equals(stored.version())) {
			throw new RepositoryException("Stored archive '" + stored.id() + "' uses cache version "
					+ stored.version() + " but this crawler requires "
					+ ArchiveVersion.CURRENT_IMPLEMENTATION_VERSION + ".");
		}
		final List<Path> configuredRoots = List.copyOf(descriptor.paths());
		final List<Bucket> storedBuckets = stored.buckets();
		if (storedBuckets.size() != configuredRoots.size()) {
			throw new RepositoryException("Stored archive '" + stored.id() + "' contains " + storedBuckets.size()
					+ " buckets but the current configuration supplies " + configuredRoots.size() + " archive roots.");
		}

		boolean unchanged = true;
		final List<Bucket> bound = new ArrayList<>(storedBuckets.size());
		for (int index = 0; index < storedBuckets.size(); index++) {
			final Bucket storedBucket = storedBuckets.get(index);
			final Path configuredRoot = configuredRoots.get(index);
			if (!normalize(Path.of(storedBucket.basePath())).equals(normalize(configuredRoot))) {
				unchanged = false;
			}
			bound.add(Bucket.of(configuredRoot, storedBucket.root()));
		}
		return unchanged ? stored : Archive.of(stored.id(), List.copyOf(bound));
	}

	private List<LocatedSubtree> locateSubtrees(final Archive stored, final Collection<Path> requestedPaths) {
		final List<Path> effectivePaths = eliminateNestedPaths(requestedPaths);
		final List<ConfiguredRoot> configuredRoots = descriptor.paths().stream()
				.map(path -> new ConfiguredRoot(normalize(path))).toList();
		final List<LocatedSubtree> result = new ArrayList<>();

		for (final Path requestedPath : effectivePaths) {
			final List<ConfiguredRoot> matchingRoots = configuredRoots.stream()
					.filter(root -> requestedPath.startsWith(root.normalized())).toList();
			if (matchingRoots.size() != 1) {
				throw new IllegalArgumentException(matchingRoots.isEmpty()
						? "Archive subtree is not below a configured root: " + requestedPath
						: "Archive subtree belongs to more than one configured root: " + requestedPath);
			}

			final ConfiguredRoot configuredRoot = matchingRoots.getFirst();
			if (!Files.isDirectory(requestedPath)) {
				throw new IllegalArgumentException(
						"Archive subtree is not an existing folder; re-index its existing parent instead: "
								+ requestedPath);
			}
			final int bucketIndex = findBucket(stored, configuredRoot.normalized());
			final List<String> relativeFolders = relativeFolders(configuredRoot.normalized(), requestedPath);
			requireStoredSubtree(stored.buckets().get(bucketIndex).root(), relativeFolders, requestedPath);
			result.add(new LocatedSubtree(bucketIndex,
					new ArchiveDigTarget(configuredRoot.normalized(), requestedPath), relativeFolders));
		}
		return result;
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
		return path.toAbsolutePath().normalize();
	}

	private static int findBucket(final Archive stored, final Path normalizedRoot) {
		final List<Integer> matches = new ArrayList<>();
		for (int index = 0; index < stored.buckets().size(); index++) {
			final Path bucketPath = normalize(Path.of(stored.buckets().get(index).basePath()));
			if (bucketPath.equals(normalizedRoot)) {
				matches.add(index);
			}
		}
		if (matches.size() != 1) {
			throw new IllegalStateException(matches.isEmpty()
					? "The stored clue archive has no bucket for configured root: " + normalizedRoot
					: "The stored clue archive has more than one bucket for configured root: " + normalizedRoot);
		}
		return matches.getFirst();
	}

	private static List<String> relativeFolders(final Path root, final Path path) {
		final List<String> result = new ArrayList<>();
		root.relativize(path).forEach(folder -> result.add(folder.toString()));
		return List.copyOf(result);
	}

	private static void requireStoredSubtree(final ArchiveNode root, final List<String> relativeFolders,
			final Path requestedPath) {
		ArchiveNode current = root;
		for (final String folder : relativeFolders) {
			current = child(current, folder)
					.orElseThrow(() -> new IllegalArgumentException(
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
				return new ArchiveNode(current.folder(), current.artifact(), replacements);
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

	private record ConfiguredRoot(Path normalized) {
	}

	private record LocatedSubtree(int bucketIndex, ArchiveDigTarget target, List<String> relativeFolders) {
	}

}
