package com.retrocrawler.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveDigger;
import com.retrocrawler.core.archive.ArchiveManager;
import com.retrocrawler.core.archive.Repository;
import com.retrocrawler.core.archive.clues.Archive;
import com.retrocrawler.core.archive.clues.ArchiveNode;
import com.retrocrawler.core.archive.clues.Artifact;
import com.retrocrawler.core.archive.clues.Bucket;
import com.retrocrawler.core.gear.GearResolution;
import com.retrocrawler.core.gear.GearResolver;
import com.retrocrawler.core.util.CrawlProgress;
import com.retrocrawler.core.util.Monitor;
import com.retrocrawler.core.util.PathNames;

public class RetroCrawlerImpl implements RetroCrawler {

	private final ArchiveDescriptor archiveDescriptor;

	private final ArchiveManager manager;

	private final GearResolver resolver;

	// package-private: only factories construct this
	RetroCrawlerImpl(final ArchiveDescriptor descriptor, final ArchiveDigger digger, final GearResolver resolver,
			final Repository repository) {
		this.archiveDescriptor = Objects.requireNonNull(descriptor, "descriptor");
		this.manager = new ArchiveManager(descriptor, digger, repository);
		this.resolver = Objects.requireNonNull(resolver, "resolver");
	}

	@Override
	public ArchiveDescriptor getArchiveDescriptor() {
		return archiveDescriptor;
	}

	@Override
	public <R, N, G> R crawl(final Monitor monitor, final boolean reindex, final GearTreeFactory<R, N, G> factory)
			throws IOException {

		Objects.requireNonNull(monitor, "monitor");
		Objects.requireNonNull(factory, "factory");
		monitor.throwIfCancelled();

		final Class<G> gearType = Objects.requireNonNull(factory.gearType(), "factory.gearType() must not return null");

		final Archive archive = manager.getArchive(monitor, reindex);
		final RetroIdRegistry retroIds = new RetroIdRegistry();
		final List<ResolvedBucket> resolvedBuckets = new ArrayList<>();
		final long artifactCount = archive.getBuckets().stream().map(Bucket::getRoot)
				.mapToLong(RetroCrawlerImpl::countArtifacts).sum();
		final ResolutionProgress resolutionProgress = new ResolutionProgress(artifactCount, monitor);

		for (final Bucket bucket : archive.getBuckets()) {
			monitor.throwIfCancelled();
			final ArchiveNode root = bucket.getRoot();
			final ResolvedArchiveNode resolvedRoot = root == null ? null
					: resolve(root, Path.of(bucket.getBasePath()), retroIds, resolutionProgress, monitor);
			resolvedBuckets.add(new ResolvedBucket(bucket, resolvedRoot));
		}

		retroIds.assertUnique();

		for (final ResolvedBucket resolvedBucket : resolvedBuckets) {
			monitor.throwIfCancelled();
			final Bucket bucket = resolvedBucket.bucket();
			factory.beginBucket(bucket);
			if (resolvedBucket.root() != null) {
				emitCompressed(resolvedBucket.root(), null, factory, gearType, monitor);
			}
			factory.endBucket(bucket);
		}

		final R result = factory.build();
		monitor.done("Crawl complete.");
		return result;
	}

	private static long countArtifacts(final ArchiveNode node) {
		if (node == null) {
			return 0;
		}
		long count = node.getArtifact() == null ? 0 : 1;
		final List<ArchiveNode> children = node.getChildren();
		if (children != null) {
			for (final ArchiveNode child : children) {
				count += countArtifacts(child);
			}
		}
		return count;
	}

	/**
	 * Emits a compressed gear tree: - If node resolves to gear of the desired type:
	 * emit factory node and pass it to children as parent - If node does not
	 * resolve or type does not match: emit nothing, keep same parent for children
	 * (lifting)
	 */
	private <R, N, G> void emitCompressed(final ResolvedArchiveNode node, final N parent,
			final GearTreeFactory<R, N, G> factory, final Class<G> gearType, final Monitor monitor) {

		monitor.throwIfCancelled();
		final Optional<Object> resolved = node.resolution().map(GearResolution::gear);

		final N nextParent;
		if (resolved.isPresent() && gearType.isInstance(resolved.get())) {
			final G typed = gearType.cast(resolved.get());
			nextParent = factory.addNode(parent, typed);
		} else {
			nextParent = parent;
		}

		final List<ResolvedArchiveNode> children = node.children();
		if (children.isEmpty()) {
			return;
		}
		for (final ResolvedArchiveNode child : children) {
			emitCompressed(child, nextParent, factory, gearType, monitor);
		}
	}

	private ResolvedArchiveNode resolve(final ArchiveNode node, final Path sourcePath, final RetroIdRegistry retroIds,
			final ResolutionProgress progress, final Monitor monitor) {
		monitor.throwIfCancelled();
		final Artifact artifact = node.getArtifact();
		final Optional<GearResolution> resolution = artifact == null ? Optional.empty()
				: resolver.resolveWithIdentity(artifact);
		resolution.ifPresent(value -> retroIds.register(value, sourcePath));
		if (artifact != null) {
			progress.complete(sourcePath);
		}

		final List<ResolvedArchiveNode> children = new ArrayList<>();
		final List<ArchiveNode> archiveChildren = node.getChildren();
		if (archiveChildren != null) {
			for (final ArchiveNode child : archiveChildren) {
				children.add(resolve(child, sourcePath.resolve(child.getFolder()), retroIds, progress, monitor));
			}
		}
		return new ResolvedArchiveNode(resolution, List.copyOf(children));
	}

	private static final class ResolutionProgress {

		private final long total;

		private final Monitor monitor;

		private long completed;

		private ResolutionProgress(final long total, final Monitor monitor) {
			this.total = total;
			this.monitor = monitor;
			final String message = total == 0 ? "No artifacts to resolve." : "Resolving " + total + " artifacts.";
			monitor.report(CrawlProgress.exact(CrawlProgress.Phase.RESOLVING, message, 0, total));
		}

		private void complete(final Path sourcePath) {
			completed++;
			monitor.report(CrawlProgress.exact(CrawlProgress.Phase.RESOLVING,
					"Resolved artifact " + completed + " of " + total + ": "
							+ PathNames.abbreviatePathName(sourcePath.toString()),
					completed, total));
		}
	}

	private record ResolvedBucket(Bucket bucket, ResolvedArchiveNode root) {
	}

	private record ResolvedArchiveNode(Optional<GearResolution> resolution, List<ResolvedArchiveNode> children) {
	}

	private static final class RetroIdRegistry {

		private final Map<Object, List<String>> occurrences = new LinkedHashMap<>();

		private void register(final GearResolution resolution, final Path sourcePath) {
			resolution.retroId()
					.ifPresent(id -> occurrences.computeIfAbsent(id, ignored -> new ArrayList<>())
							.add(sourcePath.toString()));
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
