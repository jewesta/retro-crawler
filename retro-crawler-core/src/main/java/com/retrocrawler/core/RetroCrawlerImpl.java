package com.retrocrawler.core;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveDigger;
import com.retrocrawler.core.archive.ArchiveManager;
import com.retrocrawler.core.archive.Node;
import com.retrocrawler.core.archive.ReindexScope;
import com.retrocrawler.core.archive.Repository;
import com.retrocrawler.core.archive.clues.Archive;
import com.retrocrawler.core.archive.clues.ArchiveNode;
import com.retrocrawler.core.archive.clues.Artifact;
import com.retrocrawler.core.archive.clues.Bucket;
import com.retrocrawler.core.archive.source.ArchiveFile;
import com.retrocrawler.core.archive.source.ArchiveFileAccessor;
import com.retrocrawler.core.archive.source.ArchiveFolder;
import com.retrocrawler.core.archive.source.ArchiveListing;
import com.retrocrawler.core.archive.source.ArchiveSession;
import com.retrocrawler.core.archive.source.ArchiveSource;
import com.retrocrawler.core.gear.GearResolution;
import com.retrocrawler.core.gear.GearResolver;
import com.retrocrawler.core.gear.GearTreeFactory;
import com.retrocrawler.core.gear.parser.ParseContext;
import com.retrocrawler.core.progress.ProgressAccuracy;
import com.retrocrawler.core.progress.ProgressStage;
import com.retrocrawler.core.progress.Progressor;
import com.retrocrawler.core.util.PathNames;

class RetroCrawlerImpl implements RetroCrawler {

	private final ArchiveDescriptor archiveDescriptor;

	private final ArchiveManager manager;

	private final GearResolver resolver;

	private final Configuration configuration;

	private final ArchiveSource source;

	// package-private: only factories construct this
	RetroCrawlerImpl(final ArchiveDescriptor descriptor, final ArchiveDigger digger, final GearResolver resolver,
			final Configuration configuration, final Repository repository, final ArchiveSource source) {
		this.archiveDescriptor = Objects.requireNonNull(descriptor, "descriptor");
		this.manager = new ArchiveManager(descriptor, digger, repository);
		this.resolver = Objects.requireNonNull(resolver, "resolver");
		this.configuration = Objects.requireNonNull(configuration, "configuration");
		this.source = Objects.requireNonNull(source, "source");
	}

	@Override
	public ArchiveDescriptor archiveDescriptor() {
		return archiveDescriptor;
	}

	@Override
	public <T> Optional<T> inspect(final Path sourcePath, final ArchiveFileAccessor<T> inspector) throws IOException {
		final Path requested = Objects.requireNonNull(sourcePath, "sourcePath").normalize();
		Objects.requireNonNull(inspector, "inspector");
		final Path configuredRoot = rootFor(requested);

		try (ArchiveSession session = source.open(configuredRoot)) {
			ArchiveFolder current = Objects.requireNonNull(session.root(), "session.root()");
			final Path sourceRoot = current.path().normalize();
			if (!requested.startsWith(sourceRoot)) {
				throw new IllegalArgumentException(
						"Source path '" + sourcePath + "' is not below session root '" + current.path() + "'.");
			}

			final Path relative = sourceRoot.relativize(requested);
			if (relative.toString().isEmpty()) {
				throw new NoSuchFileException(sourcePath.toString(), null,
						"Source address identifies an archive root.");
			}

			for (int index = 0; index < relative.getNameCount(); index++) {
				final ArchiveListing listing = Objects.requireNonNull(session.list(current), "session.list(folder)");
				final Path expected = current.path().resolve(relative.getName(index)).normalize();
				final boolean terminal = index == relative.getNameCount() - 1;
				if (terminal) {
					final ArchiveFile file = listing.files().stream()
							.filter(candidate -> candidate.path().normalize().equals(expected)).findFirst()
							.orElseThrow(() -> new NoSuchFileException(sourcePath.toString()));
					return session.access(file, inspector);
				}
				current = listing.folders().stream().filter(candidate -> candidate.path().normalize().equals(expected))
						.findFirst().orElseThrow(() -> new NoSuchFileException(sourcePath.toString()));
			}
		}
		throw new NoSuchFileException(sourcePath.toString());
	}

	private Path rootFor(final Path sourcePath) {
		return archiveDescriptor.paths().stream().filter(root -> sourcePath.startsWith(root.normalize()))
				.max(Comparator.comparingInt(root -> root.normalize().getNameCount()))
				.orElseThrow(() -> new IllegalArgumentException(
						"Source path is outside the configured archives: " + sourcePath));
	}

	@Override
	public <R, N, G> R crawl(final Progressor progressor, final ReindexScope reindexScope,
			final GearTreeFactory<R, N, G> factory) throws IOException {

		Objects.requireNonNull(progressor, "progressor");
		Objects.requireNonNull(reindexScope, "reindexScope");
		Objects.requireNonNull(factory, "factory");
		progressor.throwIfCancelled();

		try {
			return crawlToCompletion(progressor, reindexScope, factory);
		} catch (final IOException | RuntimeException failure) {
			reportFailure(progressor, failure);
			throw failure;
		}
	}

	private <R, N, G> R crawlToCompletion(final Progressor progressor, final ReindexScope reindexScope,
			final GearTreeFactory<R, N, G> factory) throws IOException {
		final Class<G> gearType = Objects.requireNonNull(factory.gearType(), "factory.gearType() must not return null");

		final Archive archive = manager.archive(progressor, reindexScope);
		final RetroIdRegistry retroIds = new RetroIdRegistry();
		final List<ResolvedBucket> resolvedBuckets = new ArrayList<>();
		final long artifactCount = archive.buckets().stream().map(Bucket::root)
				.mapToLong(RetroCrawlerImpl::countArtifacts).sum();
		final ResolutionProgress resolutionProgress = new ResolutionProgress(artifactCount, progressor);

		for (final Bucket bucket : archive.buckets()) {
			progressor.throwIfCancelled();
			final ArchiveNode root = bucket.root();
			final Path archiveRoot = Path.of(bucket.basePath());
			final ResolvedArchiveNode resolvedRoot = root == null ? null
					: resolve(root, archiveRoot, archiveRoot, retroIds, resolutionProgress, progressor);
			resolvedBuckets.add(new ResolvedBucket(bucket, resolvedRoot));
		}

		retroIds.assertUnique();

		for (final ResolvedBucket resolvedBucket : resolvedBuckets) {
			progressor.throwIfCancelled();
			final Bucket bucket = resolvedBucket.bucket();
			factory.beginBucket(bucket);
			if (resolvedBucket.root() != null) {
				emitCompressed(resolvedBucket.root(), null, factory, gearType, progressor);
			}
			factory.endBucket(bucket);
		}

		final R result = factory.build();
		progressor.complete("Crawl complete.");
		return result;
	}

	private static String failureDescription(final Throwable failure) {
		final String message = failure.getMessage();
		return message == null || message.isBlank() ? failure.getClass().getSimpleName() : message;
	}

	private static void reportFailure(final Progressor progressor, final Throwable failure) {
		try {
			progressor.fail("Crawl failed: " + failureDescription(failure));
		} catch (final RuntimeException reportingFailure) {
			if (reportingFailure != failure) {
				failure.addSuppressed(reportingFailure);
			}
		}
	}

	private static long countArtifacts(final ArchiveNode node) {
		if (node == null) {
			return 0;
		}
		long count = node.artifact() == null ? 0 : 1;
		final List<ArchiveNode> children = node.children();
		if (children != null) {
			for (final ArchiveNode child : children) {
				count += countArtifacts(child);
			}
		}
		return count;
	}

	/**
	 * Emits a compressed gear tree: - If node resolves to gear of the desired
	 * type: emit factory node and pass it to children as parent - If node does
	 * not resolve or type does not match: emit nothing, keep same parent for
	 * children (lifting)
	 */
	private <R, N, G> void emitCompressed(final ResolvedArchiveNode node, final N parent,
			final GearTreeFactory<R, N, G> factory, final Class<G> gearType, final Progressor progressor) {

		progressor.throwIfCancelled();
		final Optional<Object> resolved = node.resolution().map(GearResolution::gear);

		final N nextParent;
		if (resolved.isPresent() && gearType.isInstance(resolved.get())) {
			final G typed = gearType.cast(resolved.get());
			nextParent = factory.addNode(parent, typed, node.sourcePath());
		} else {
			nextParent = parent;
		}

		final List<ResolvedArchiveNode> children = node.children();
		if (children.isEmpty()) {
			return;
		}
		for (final ResolvedArchiveNode child : children) {
			emitCompressed(child, nextParent, factory, gearType, progressor);
		}
	}

	private ResolvedArchiveNode resolve(final ArchiveNode node, final Path archiveRoot, final Path sourcePath,
			final RetroIdRegistry retroIds, final ResolutionProgress progress, final Progressor progressor) {
		progressor.throwIfCancelled();
		final Artifact artifact = node.artifact();
		final Optional<GearResolution> resolution = artifact == null ? Optional.empty()
				: resolver.resolveWithIdentity(artifact,
						new ParseContext(configuration, new Node(archiveRoot, sourcePath)));
		resolution.ifPresent(value -> retroIds.register(value, sourcePath));
		if (artifact != null) {
			progress.complete(sourcePath);
		}

		final List<ResolvedArchiveNode> children = new ArrayList<>();
		final List<ArchiveNode> archiveChildren = node.children();
		if (archiveChildren != null) {
			for (final ArchiveNode child : archiveChildren) {
				children.add(resolve(child, archiveRoot, sourcePath.resolve(child.folder()), retroIds, progress,
						progressor));
			}
		}
		return new ResolvedArchiveNode(resolution, sourcePath, List.copyOf(children));
	}

	private static final class ResolutionProgress {

		private final long total;

		private final Progressor progressor;

		private long completed;

		private ResolutionProgress(final long total, final Progressor progressor) {
			this.total = total;
			this.progressor = progressor;
			final String message = total == 0 ? "No artifacts to resolve." : "Resolving " + total + " artifacts.";
			progressor.begin(ProgressStage.RESOLVING, message, total, ProgressAccuracy.EXACT);
		}

		private void complete(final Path sourcePath) {
			completed++;
			progressor.advanceTo(completed, "Resolved artifact " + completed + " of " + total + ": "
					+ PathNames.abbreviatePathName(sourcePath.toString()));
		}
	}

	private record ResolvedBucket(Bucket bucket, ResolvedArchiveNode root) {
	}

	private record ResolvedArchiveNode(Optional<GearResolution> resolution, Path sourcePath,
			List<ResolvedArchiveNode> children) {
	}

	private static final class RetroIdRegistry {

		private final Map<Object, List<String>> occurrences = new LinkedHashMap<>();

		private void register(final GearResolution resolution, final Path sourcePath) {
			resolution.retroId().ifPresent(
					id -> occurrences.computeIfAbsent(id, ignored -> new ArrayList<>()).add(sourcePath.toString()));
		}

		private void assertUnique() {
			final Map<Object, List<String>> duplicates = new LinkedHashMap<>();
			occurrences.forEach((id, paths) -> {
				if (paths.size() > 1) {
					duplicates.put(id, List.copyOf(paths));
				}
			});
			if (!duplicates.isEmpty()) {
				throw new DuplicateRetroIdException(duplicates);
			}
		}
	}
}
