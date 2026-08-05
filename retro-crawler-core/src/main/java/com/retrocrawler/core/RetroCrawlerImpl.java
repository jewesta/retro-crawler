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
import java.util.function.Supplier;

import com.retrocrawler.core.archive.ARI;
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
import com.retrocrawler.core.archive.clues.DuplicateClueException;
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

	private final String collectionId;

	private final GearResolver resolver;

	private final Configuration configuration;

	// package-private: only factories construct this
	RetroCrawlerImpl(final Model model, final List<ArchiveBinding> archiveBindings, final CrawlPlanning planning,
			final Repository repository) {
		Objects.requireNonNull(model, "model");
		Objects.requireNonNull(archiveBindings, "archiveBindings");
		Objects.requireNonNull(planning, "planning");
		Objects.requireNonNull(repository, "repository");
		this.collectionId = model.collectionId();
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
	public String collectionId() {
		return collectionId;
	}

	@Override
	public ArchiveDescriptor archive(final ArchiveId archiveId) {
		return registeredArchive(archiveId).descriptor();
	}

	@Override
	public ARI identify(final ArchiveId archiveId, final Path sourcePath) {
		final RegisteredArchive archive = registeredArchive(archiveId);
		final Path requested = Objects.requireNonNull(sourcePath, "sourcePath").normalize();
		final Path configuredRoot = archive.descriptor().root().normalize();
		if (!requested.startsWith(configuredRoot)) {
			throw new IllegalArgumentException(
					"Source path is outside archive '" + archive.descriptor().id() + "': " + sourcePath);
		}
		return ARI.of(collectionId, archiveId, configuredRoot.relativize(requested));
	}

	@Override
	public <T> Optional<T> inspect(final ARI source, final ArchiveFileAccessor<T> inspector) throws IOException {
		final RegisteredArchive archive = registeredArchive(source);
		Objects.requireNonNull(inspector, "inspector");
		final Path configuredRoot = archive.descriptor().root();

		try (ArchiveSession session = archive.source().open(configuredRoot)) {
			ArchiveFolder current = Objects.requireNonNull(session.root(), "session.root()");
			final Path relative = source.resourcePath();
			for (int index = 0; index < relative.getNameCount(); index++) {
				final Path expected = current.path().resolve(relative.getName(index).toString()).normalize();
				final boolean last = index == relative.getNameCount() - 1;
				final ArchiveListing listing = Objects.requireNonNull(session.list(current), "session.list(folder)");
				if (last) {
					final Optional<ArchiveFile> file = listing.files().stream()
							.filter(candidate -> candidate.path().normalize().equals(expected)).findFirst();
					if (file.isPresent()) {
						return session.access(file.get(), inspector);
					}
					throw new NoSuchFileException(source.toString());
				}
				final Optional<ArchiveFolder> folder = listing.folders().stream()
						.filter(candidate -> candidate.path().normalize().equals(expected)).findFirst();
				if (folder.isEmpty()) {
					throw new NoSuchFileException(source.toString());
				}
				current = folder.get();
			}
		}
		throw new NoSuchFileException(source.toString());
	}

	private RegisteredArchive registeredArchive(final ArchiveId archiveId) {
		Objects.requireNonNull(archiveId, "archiveId");
		final RegisteredArchive archive = archives.get(archiveId);
		if (archive == null) {
			throw new IllegalArgumentException("Unknown archive: " + archiveId);
		}
		return archive;
	}

	private RegisteredArchive registeredArchive(final ARI source) {
		Objects.requireNonNull(source, "source");
		if (!collectionId.equals(source.collectionId())) {
			throw new IllegalArgumentException("ARI belongs to collection '" + source.collectionId()
					+ "' but this crawler represents collection '" + collectionId + "': " + source);
		}
		return registeredArchive(source.archiveId());
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
		requireRoutableSubtrees(selected, reindexScope);

		final List<CrawledArchive> crawled = new ArrayList<>();
		for (final RegisteredArchive registered : selected) {
			progressor.throwIfCancelled();
			final ReindexScope scope = routedScope(registered.descriptor(), reindexScope);
			crawled.add(new CrawledArchive(registered.descriptor(), registered.manager().archive(progressor, scope)));
		}

		final RetroIdRegistry retroIds = new RetroIdRegistry();
		final long artifactCount = crawled.stream().mapToLong(archive -> countArtifacts(archive.clues().root())).sum();
		final ResolutionProgress resolutionProgress = new ResolutionProgress(artifactCount, progressor);

		final List<ResolvedArchive> resolvedArchives = new ArrayList<>();
		for (final CrawledArchive archive : crawled) {
			progressor.throwIfCancelled();
			final Path archiveRoot = Path.of(archive.clues().basePath());
			final ResolvedArchiveNode resolvedRoot = resolve(archive.descriptor().id(), archive.clues().root(),
					archiveRoot, archiveRoot, retroIds, resolutionProgress, progressor);
			resolvedArchives.add(new ResolvedArchive(archive.descriptor(), resolvedRoot));
		}

		retroIds.assertUnique();

		for (final ResolvedArchive resolvedArchive : resolvedArchives) {
			progressor.throwIfCancelled();
			factory.beginArchive(resolvedArchive.descriptor());
			emitCompressed(resolvedArchive.descriptor().id(), resolvedArchive.root(), null, factory, gearType,
					progressor);
			factory.endArchive(resolvedArchive.descriptor());
		}

		final R result = factory.build();
		progressor.complete("Crawl complete.");
		return result;
	}

	private void requireRoutableSubtrees(final Collection<RegisteredArchive> selected,
			final ReindexScope reindexScope) {
		if (reindexScope.kind() != ReindexScope.Kind.SUBTREES) {
			return;
		}
		for (final ARI subtree : reindexScope.subtrees()) {
			registeredArchive(subtree);
			final boolean selectedArchive = selected.stream()
					.anyMatch(archive -> subtree.archiveId().equals(archive.descriptor().id()));
			if (!selectedArchive) {
				throw new IllegalArgumentException(
						"ARI does not belong to an archive selected for this crawl: " + subtree);
			}
		}
	}

	private static ReindexScope routedScope(final ArchiveDescriptor descriptor, final ReindexScope reindexScope) {
		if (reindexScope.kind() != ReindexScope.Kind.SUBTREES) {
			return reindexScope;
		}
		final List<ARI> routed = reindexScope.subtrees().stream()
				.filter(subtree -> descriptor.id().equals(subtree.archiveId())).toList();
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
	private <R, N, G> void emitCompressed(final ArchiveId archiveId, final ResolvedArchiveNode node, final N parent,
			final GearTreeFactory<R, N, G> factory, final Class<G> gearType, final Progressor progressor) {

		progressor.throwIfCancelled();
		final Optional<Object> resolved = node.resolution().map(GearResolution::gear);

		final N nextParent;
		if (resolved.isPresent() && gearType.isInstance(resolved.get())) {
			final G typed = gearType.cast(resolved.get());
			final ARI source = ARI.of(collectionId, archiveId, node.relativeSourcePath());
			nextParent = factory.addNode(parent, typed, source);
		} else {
			nextParent = parent;
		}

		final List<ResolvedArchiveNode> children = node.children();
		if (children.isEmpty()) {
			return;
		}
		for (final ResolvedArchiveNode child : children) {
			emitCompressed(archiveId, child, nextParent, factory, gearType, progressor);
		}
	}

	/**
	 * Names the artifact whose clues could not be interpreted.
	 * <p>
	 * A clue conflict discovered here is not a finder failure: the archive was
	 * crawled cleanly and only the model's vocabulary reveals that two clues
	 * claim one semantic key. The phase differs, so the exception type does too,
	 * but the header reads the same as a crawl-time report.
	 */
	private static <T> T resolving(final Path relativeSourcePath, final Supplier<T> resolve) {
		try {
			return resolve.get();
		} catch (final DuplicateClueException conflict) {
			throw new DuplicateClueException(
					portable(relativeSourcePath) + ": Resolving clues into facts.\n" + conflict.getMessage(), conflict);
		}
	}

	private static String portable(final Path path) {
		final String separator = path.getFileSystem().getSeparator();
		final String value = path.toString();
		final String normalized = "/".equals(separator) ? value : value.replace(separator, "/");
		return normalized.isEmpty() ? "." : normalized;
	}

	private ResolvedArchiveNode resolve(final ArchiveId archiveId, final ArchiveNode node, final Path archiveRoot,
			final Path sourcePath, final RetroIdRegistry retroIds, final ResolutionProgress progress,
			final Progressor progressor) {
		progressor.throwIfCancelled();
		final Artifact artifact = node.artifact();
		final Path relativeSourcePath = archiveRoot.relativize(sourcePath);
		final Optional<GearResolution> resolution = artifact == null ? Optional.empty()
				: resolving(relativeSourcePath, () -> resolver.resolveWithIdentity(artifact,
						new ParseContext(configuration, new Node(archiveRoot, sourcePath))));
		resolution.ifPresent(value -> value.retroId()
				.ifPresent(id -> retroIds.register(id, ARI.of(collectionId, archiveId, relativeSourcePath))));
		if (artifact != null) {
			progress.complete(sourcePath);
		}

		final List<ResolvedArchiveNode> children = new ArrayList<>();
		final List<ArchiveNode> archiveChildren = node.children();
		if (archiveChildren != null) {
			for (final ArchiveNode child : archiveChildren) {
				children.add(resolve(archiveId, child, archiveRoot, sourcePath.resolve(child.folder()), retroIds,
						progress, progressor));
			}
		}
		return new ResolvedArchiveNode(resolution, relativeSourcePath, List.copyOf(children));
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

	private record ResolvedArchiveNode(Optional<GearResolution> resolution,
			// Retained as a Path to avoid constructing an ARI for every archive node.
			Path relativeSourcePath, List<ResolvedArchiveNode> children) {
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

		private void register(final Object retroId, final ARI source) {
			occurrences.computeIfAbsent(retroId, ignored -> new ArrayList<>()).add(source.toString());
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
