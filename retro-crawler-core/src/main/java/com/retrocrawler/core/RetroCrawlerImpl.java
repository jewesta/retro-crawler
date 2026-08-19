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

import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.archive.ArchiveDefinition;
import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveDigger;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.archive.ArchiveManager;
import com.retrocrawler.core.archive.ArtifactLocation;
import com.retrocrawler.core.archive.CrawlPlanning;
import com.retrocrawler.core.archive.ReindexScope;
import com.retrocrawler.core.archive.Repository;
import com.retrocrawler.core.archive.clues.Archive;
import com.retrocrawler.core.archive.clues.ArchiveNode;
import com.retrocrawler.core.archive.clues.Artifact;
import com.retrocrawler.core.archive.clues.ClueFinder;
import com.retrocrawler.core.archive.filter.ArchivePathFilter;
import com.retrocrawler.core.archive.source.ArchiveFile;
import com.retrocrawler.core.archive.source.ArchiveFileAccessor;
import com.retrocrawler.core.archive.source.ArchiveFolder;
import com.retrocrawler.core.archive.source.ArchiveListing;
import com.retrocrawler.core.archive.source.ArchiveSession;
import com.retrocrawler.core.archive.source.ArchiveSource;
import com.retrocrawler.core.gear.GearResolution;
import com.retrocrawler.core.gear.GearResolutionException;
import com.retrocrawler.core.gear.GearResolver;
import com.retrocrawler.core.gear.GearTreeFactory;
import com.retrocrawler.core.gear.parser.ParseContext;
import com.retrocrawler.core.progress.ProgressAccuracy;
import com.retrocrawler.core.progress.ProgressCancelledException;
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
		return assertArchive(archiveId).descriptor();
	}

	@Override
	public ARI identify(final ArchiveId archiveId, final Path sourcePath) {
		final RegisteredArchive archive = assertArchive(archiveId);
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
		final RegisteredArchive archive = assertArchive(source);
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

	private RegisteredArchive assertArchive(final ArchiveId archiveId) {
		Objects.requireNonNull(archiveId, "archiveId");
		final RegisteredArchive archive = archives.get(archiveId);
		if (archive == null) {
			throw new IllegalArgumentException("Unknown archive: " + archiveId);
		}
		return archive;
	}

	private RegisteredArchive assertArchive(final ARI source) {
		Objects.requireNonNull(source, "source");
		if (!collectionId.equals(source.collectionId())) {
			throw new IllegalArgumentException("ARI belongs to collection '" + source.collectionId()
					+ "' but this crawler represents collection '" + collectionId + "': " + source);
		}
		return assertArchive(source.archiveId());
	}

	@Override
	public <R, N, G> R crawl(final ArchiveId archiveId, final Journal journal, final ReindexScope reindexScope,
			final GearTreeFactory<R, N, G> factory) throws IOException {
		return crawl(List.of(assertArchive(archiveId)), journal, reindexScope, factory);
	}

	@Override
	public <R, N, G> R crawlAll(final Journal journal, final ReindexScope reindexScope,
			final GearTreeFactory<R, N, G> factory) throws IOException {
		return crawl(archives.values(), journal, reindexScope, factory);
	}

	private <R, N, G> R crawl(final Collection<RegisteredArchive> selected, final Journal journal,
			final ReindexScope reindexScope, final GearTreeFactory<R, N, G> factory) throws IOException {

		Objects.requireNonNull(journal, "journal");
		Objects.requireNonNull(reindexScope, "reindexScope");
		Objects.requireNonNull(factory, "factory");
		return journal.track("Crawl", () -> crawlToCompletion(selected, journal, reindexScope, factory));
	}

	private <R, N, G> R crawlToCompletion(final Collection<RegisteredArchive> selected, final Journal journal,
			final ReindexScope reindexScope, final GearTreeFactory<R, N, G> factory) throws IOException {
		final Class<G> gearType = Objects.requireNonNull(factory.gearType(), "factory.gearType() must not return null");
		requireRoutableSubtrees(selected, reindexScope);

		final List<CrawledArchive> crawled = new ArrayList<>();
		for (final RegisteredArchive registered : selected) {
			journal.throwIfCancelled();
			final ReindexScope scope = routedScope(registered.descriptor(), reindexScope);
			final int failuresBeforeArchive = journal.failureCount();
			try {
				crawled.add(new CrawledArchive(registered.descriptor(), registered.manager().archive(journal, scope)));
			} catch (final CrawlException failure) {
				if (journal.failureMode() == FailureMode.FAIL_EARLY
						|| journal.failureCount() == failuresBeforeArchive) {
					throw failure;
				}
			}
		}

		final RetroIdRegistry retroIds = new RetroIdRegistry();
		final long artifactCount = crawled.stream().mapToLong(archive -> countArtifacts(archive.clues().root())).sum();
		final String resolutionMessage = artifactCount == 0 ? "No artifacts to resolve."
				: "Resolving " + artifactCount + " artifacts.";
		journal.begin(CrawlProgressStages.RESOLVING, resolutionMessage, artifactCount, ProgressAccuracy.EXACT);

		final List<ResolvedArchive> resolvedArchives = new ArrayList<>();
		for (final CrawledArchive archive : crawled) {
			journal.throwIfCancelled();
			final Path archiveRoot = Path.of(archive.clues().basePath());
			final ResolvedArchiveNode resolvedRoot = resolve(archive.descriptor().id(), archive.clues().root(),
					archiveRoot, archiveRoot, retroIds, journal);
			resolvedArchives.add(new ResolvedArchive(archive.descriptor(), resolvedRoot));
		}

		try {
			retroIds.assertUnique();
		} catch (final DuplicateRetroIdException failure) {
			journal.record(failure);
		}
		if (journal.hasFailures()) {
			throw new CrawlException(journal.failures());
		}

		for (final ResolvedArchive resolvedArchive : resolvedArchives) {
			journal.throwIfCancelled();
			factory.beginArchive(resolvedArchive.descriptor());
			emitCompressed(resolvedArchive.descriptor().id(), resolvedArchive.root(), null, factory, gearType, journal);
			factory.endArchive(resolvedArchive.descriptor());
		}

		return factory.build();
	}

	private void requireRoutableSubtrees(final Collection<RegisteredArchive> selected,
			final ReindexScope reindexScope) {
		if (reindexScope.kind() != ReindexScope.Kind.SUBTREES) {
			return;
		}
		for (final ARI subtree : reindexScope.subtrees()) {
			assertArchive(subtree);
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
			final GearTreeFactory<R, N, G> factory, final Class<G> gearType, final Journal journal) {

		journal.throwIfCancelled();
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
			emitCompressed(archiveId, child, nextParent, factory, gearType, journal);
		}
	}

	private Optional<GearResolution> resolveArtifact(final ARI source, final Artifact artifact, final Path archiveRoot,
			final Path sourcePath, final Journal journal) {
		try {
			return resolver.resolveWithIdentity(source, artifact,
					new ParseContext(configuration, new ArtifactLocation(archiveRoot, sourcePath)));
		} catch (final ProgressCancelledException cancellation) {
			throw cancellation;
		} catch (final RuntimeException failure) {
			journal.record(new GearResolutionException(source, failure));
			return Optional.empty();
		} finally {
			journal.advance("Examined artifact: " + PathNames.abbreviatePathName(sourcePath.toString()));
		}
	}

	private ResolvedArchiveNode resolve(final ArchiveId archiveId, final ArchiveNode node, final Path archiveRoot,
			final Path sourcePath, final RetroIdRegistry retroIds, final Journal journal) {
		journal.throwIfCancelled();
		final Artifact artifact = node.artifact();
		final Path relativeSourcePath = archiveRoot.relativize(sourcePath);
		final Optional<GearResolution> resolution;
		if (artifact == null) {
			resolution = Optional.empty();
		} else {
			final ARI source = ARI.of(collectionId, archiveId, relativeSourcePath);
			resolution = resolveArtifact(source, artifact, archiveRoot, sourcePath, journal);
			resolution.ifPresent(value -> value.retroId().ifPresent(id -> retroIds.register(id, source)));
		}

		final List<ResolvedArchiveNode> children = new ArrayList<>();
		final List<ArchiveNode> archiveChildren = node.children();
		if (archiveChildren != null) {
			for (final ArchiveNode child : archiveChildren) {
				children.add(
						resolve(archiveId, child, archiveRoot, sourcePath.resolve(child.folder()), retroIds, journal));
			}
		}
		return new ResolvedArchiveNode(resolution, relativeSourcePath, List.copyOf(children));
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
		public List<ClueFinder> clueFinders() {
			return model.clueFinders();
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
