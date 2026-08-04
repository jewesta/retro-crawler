package com.retrocrawler.core;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.retrocrawler.core.archive.ArchiveDefinition;
import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveDigger;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.archive.ArchiveManager;
import com.retrocrawler.core.archive.CrawlPlanning;
import com.retrocrawler.core.archive.Node;
import com.retrocrawler.core.archive.ReindexScope;
import com.retrocrawler.core.archive.Repository;
import com.retrocrawler.core.archive.clues.Archive;
import com.retrocrawler.core.archive.clues.ArchiveNode;
import com.retrocrawler.core.archive.clues.ArchivePathClueFinder;
import com.retrocrawler.core.archive.clues.Artifact;
import com.retrocrawler.core.archive.filter.ArchivePathFilter;
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

	private final List<ArchiveDescriptor> archiveDescriptors;

	private final Map<ArchiveId, RegisteredArchive> archives;

	private final GearResolver resolver;

	private final Configuration configuration;

	// package-private: only factories construct this
	RetroCrawlerImpl(final Model model, final List<ArchiveBinding> archiveBindings, final CrawlPlanning planning,
			final Repository repository) {
		Objects.requireNonNull(model, "model");
		Objects.requireNonNull(archiveBindings, "archiveBindings");
		Objects.requireNonNull(planning, "planning");
		Objects.requireNonNull(repository, "repository");
		this.resolver = model.gearResolver();
		this.configuration = model.configuration();

		final Map<ArchiveId, RegisteredArchive> configured = new LinkedHashMap<>();
		for (final ArchiveBinding binding : archiveBindings) {
			final ArchiveDescriptor descriptor = binding.descriptor();
			final ArchiveDigger digger = new ArchiveDigger(new ModelArchiveDefinition(descriptor, model),
					binding.source(), planning);
			final RegisteredArchive archive = new RegisteredArchive(descriptor,
					new ArchiveManager(descriptor, digger, repository), binding.source());
			if (configured.putIfAbsent(descriptor.id(), archive) != null) {
				throw new IllegalArgumentException("Archive is already configured: " + descriptor.id());
			}
		}
		if (configured.isEmpty()) {
			throw new IllegalArgumentException("At least one archive must be configured.");
		}
		this.archives = Collections.unmodifiableMap(configured);
		this.archiveDescriptors = configured.values().stream().map(RegisteredArchive::descriptor).toList();
	}

	@Override
	public List<ArchiveDescriptor> archives() {
		return archiveDescriptors;
	}

	@Override
	public ArchiveDescriptor archive(final ArchiveId archiveId) {
		return registeredArchive(archiveId).descriptor();
	}

	@Override
	public <T> Optional<T> inspect(final ArchiveId archiveId, final Path sourcePath,
			final ArchiveFileAccessor<T> inspector) throws IOException {
		final RegisteredArchive archive = registeredArchive(archiveId);
		final Path requested = Objects.requireNonNull(sourcePath, "sourcePath").normalize();
		Objects.requireNonNull(inspector, "inspector");
		final Path configuredRoot = archive.descriptor().root();
		if (!requested.startsWith(configuredRoot.normalize())) {
			throw new IllegalArgumentException(
					"Source path is outside archive '" + archive.descriptor().id() + "': " + sourcePath);
		}

		try (ArchiveSession session = archive.source().open(configuredRoot)) {
			ArchiveFolder current = Objects.requireNonNull(session.root(), "session.root()");
			final Path sourceRoot = current.path().normalize();
			if (!requested.startsWith(sourceRoot)) {
				throw new IllegalArgumentException("Source path is outside the opened archive root: " + sourcePath);
			}

			final Path relative = sourceRoot.relativize(requested);
			for (int index = 0; index < relative.getNameCount(); index++) {
				final Path expected = current.path().resolve(relative.getName(index)).normalize();
				final boolean last = index == relative.getNameCount() - 1;
				final ArchiveListing listing = Objects.requireNonNull(session.list(current), "session.list(folder)");
				if (last) {
					final Optional<ArchiveFile> file = listing.files().stream()
							.filter(candidate -> candidate.path().normalize().equals(expected)).findFirst();
					if (file.isPresent()) {
						return session.access(file.get(), inspector);
					}
					throw new NoSuchFileException(sourcePath.toString());
				}
				final Optional<ArchiveFolder> folder = listing.folders().stream()
						.filter(candidate -> candidate.path().normalize().equals(expected)).findFirst();
				if (folder.isEmpty()) {
					throw new NoSuchFileException(sourcePath.toString());
				}
				current = folder.get();
			}
		}
		throw new NoSuchFileException(sourcePath.toString());
	}

	private RegisteredArchive registeredArchive(final ArchiveId archiveId) {
		Objects.requireNonNull(archiveId, "archiveId");
		final RegisteredArchive archive = archives.get(archiveId);
		if (archive == null) {
			throw new IllegalArgumentException("Unknown archive: " + archiveId);
		}
		return archive;
	}

	@Override
	public <R, N, G> R crawl(final ArchiveId archiveId, final Progressor progressor, final ReindexScope reindexScope,
			final GearTreeFactory<R, N, G> factory) throws IOException {
		return crawl(List.of(registeredArchive(archiveId)), progressor, reindexScope, factory);
	}

	@Override
	public <R, N, G> R crawlAll(final Progressor progressor, final ReindexScope reindexScope,
			final GearTreeFactory<R, N, G> factory) throws IOException {
		return crawl(archives.values(), progressor, reindexScope, factory);
	}

	private <R, N, G> R crawl(final Collection<RegisteredArchive> selected, final Progressor progressor,
			final ReindexScope reindexScope, final GearTreeFactory<R, N, G> factory) throws IOException {

		Objects.requireNonNull(progressor, "progressor");
		Objects.requireNonNull(reindexScope, "reindexScope");
		Objects.requireNonNull(factory, "factory");
		progressor.throwIfCancelled();

		try {
			return crawlToCompletion(selected, progressor, reindexScope, factory);
		} catch (final IOException | RuntimeException failure) {
			reportFailure(progressor, failure);
			throw failure;
		}
	}

	private <R, N, G> R crawlToCompletion(final Collection<RegisteredArchive> selected, final Progressor progressor,
			final ReindexScope reindexScope, final GearTreeFactory<R, N, G> factory) throws IOException {
		final Class<G> gearType = Objects.requireNonNull(factory.gearType(), "factory.gearType() must not return null");
		final boolean route = selected.size() > 1;
		if (route) {
			requireRoutableSubtrees(selected, reindexScope);
		}

		final List<CrawledArchive> crawled = new ArrayList<>();
		for (final RegisteredArchive registered : selected) {
			progressor.throwIfCancelled();
			final ReindexScope scope = route ? routedScope(registered.descriptor(), reindexScope) : reindexScope;
			crawled.add(new CrawledArchive(registered.descriptor(), registered.manager().archive(progressor, scope)));
		}

		final RetroIdRegistry retroIds = new RetroIdRegistry();
		final long artifactCount = crawled.stream().mapToLong(archive -> countArtifacts(archive.clues().root())).sum();
		final ResolutionProgress resolutionProgress = new ResolutionProgress(artifactCount, progressor);

		final List<ResolvedArchive> resolvedArchives = new ArrayList<>();
		for (final CrawledArchive archive : crawled) {
			progressor.throwIfCancelled();
			final Path archiveRoot = Path.of(archive.clues().basePath());
			final ResolvedArchiveNode resolvedRoot = resolve(archive.clues().root(), archiveRoot, archiveRoot, retroIds,
					resolutionProgress, progressor);
			resolvedArchives.add(new ResolvedArchive(archive.descriptor(), resolvedRoot));
		}

		retroIds.assertUnique();

		for (final ResolvedArchive resolvedArchive : resolvedArchives) {
			progressor.throwIfCancelled();
			factory.beginArchive(resolvedArchive.descriptor());
			emitCompressed(resolvedArchive.root(), null, factory, gearType, progressor);
			factory.endArchive(resolvedArchive.descriptor());
		}

		final R result = factory.build();
		progressor.complete("Crawl complete.");
		return result;
	}

	private static void requireRoutableSubtrees(final Collection<RegisteredArchive> selected,
			final ReindexScope reindexScope) {
		if (reindexScope.kind() != ReindexScope.Kind.SUBTREES) {
			return;
		}
		for (final Path requestedPath : reindexScope.paths()) {
			final Path normalized = requestedPath.normalize();
			final boolean routable = selected.stream()
					.anyMatch(archive -> normalized.startsWith(archive.descriptor().root().normalize()));
			if (!routable) {
				throw new IllegalArgumentException(
						"Archive subtree is not below the root of any registered archive: " + requestedPath);
			}
		}
	}

	private static ReindexScope routedScope(final ArchiveDescriptor descriptor, final ReindexScope reindexScope) {
		if (reindexScope.kind() != ReindexScope.Kind.SUBTREES) {
			return reindexScope;
		}
		final Path root = descriptor.root().normalize();
		final List<Path> routed = reindexScope.paths().stream().filter(path -> path.normalize().startsWith(root))
				.toList();
		return routed.isEmpty() ? ReindexScope.none() : ReindexScope.subtrees(routed);
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

	private record CrawledArchive(ArchiveDescriptor descriptor, Archive clues) {
	}

	private record ResolvedArchive(ArchiveDescriptor descriptor, ResolvedArchiveNode root) {
	}

	private record ResolvedArchiveNode(Optional<GearResolution> resolution, Path sourcePath,
			List<ResolvedArchiveNode> children) {
	}

	private record RegisteredArchive(ArchiveDescriptor descriptor, ArchiveManager manager, ArchiveSource source) {
	}

	private record ModelArchiveDefinition(ArchiveDescriptor archiveDescriptor, Model model)
			implements ArchiveDefinition {

		@Override
		public ArchivePathClueFinder archivePathClueFinder() {
			return model.archivePathClueFinder();
		}

		@Override
		public List<ArchivePathFilter> pathFilters() {
			return model.pathFilters();
		}
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
