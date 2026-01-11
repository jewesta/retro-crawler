package com.retrocrawler.core;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveDigger;
import com.retrocrawler.core.archive.ArchiveManager;
import com.retrocrawler.core.archive.clues.Archive;
import com.retrocrawler.core.archive.clues.ArchiveNode;
import com.retrocrawler.core.archive.clues.Artifact;
import com.retrocrawler.core.archive.clues.Bucket;
import com.retrocrawler.core.gear.GearResolver;
import com.retrocrawler.core.util.Monitor;

public class RetroCrawlerImpl implements RetroCrawler {

	private final ArchiveDescriptor archiveDescriptor;

	private final ArchiveManager manager;

	private final GearResolver resolver;

	// package-private: only factories construct this
	RetroCrawlerImpl(final ArchiveDescriptor descriptor, final ArchiveDigger digger, final GearResolver resolver) {
		this.archiveDescriptor = Objects.requireNonNull(descriptor, "descriptor");
		this.manager = new ArchiveManager(descriptor, digger);
		this.resolver = Objects.requireNonNull(resolver, "resolver");
	}

	@Override
	public ArchiveDescriptor getArchiveDescriptor() {
		return archiveDescriptor;
	}

	@Override
	public <R, N, G> R crawl(final Monitor monitor, final boolean reindex, final GearTreeFactory<R, N, G> factory)
			throws IOException {

		Objects.requireNonNull(factory, "factory");

		final Class<G> gearType = Objects.requireNonNull(factory.gearType(), "factory.gearType() must not return null");

		final Archive archive = manager.getArchive(monitor, reindex);

		for (final Bucket bucket : archive.getBuckets()) {
			factory.beginBucket(bucket);

			final ArchiveNode root = bucket.getRoot();
			if (root != null) {
				emitCompressed(root, null, factory, gearType);
			}

			factory.endBucket(bucket);
		}

		return factory.build();
	}

	/**
	 * Emits a compressed gear tree: - If node resolves to gear of the desired type:
	 * emit factory node and pass it to children as parent - If node does not
	 * resolve or type does not match: emit nothing, keep same parent for children
	 * (lifting)
	 */
	private <R, N, G> void emitCompressed(final ArchiveNode node, final N parent,
			final GearTreeFactory<R, N, G> factory, final Class<G> gearType) {

		final Optional<Object> resolved = resolveGear(node.getArtifact());

		final N nextParent;
		if (resolved.isPresent() && gearType.isInstance(resolved.get())) {
			final G typed = gearType.cast(resolved.get());
			nextParent = factory.addNode(parent, typed);
		} else {
			nextParent = parent;
		}

		final List<ArchiveNode> children = node.getChildren();
		if (children == null || children.isEmpty()) {
			return;
		}
		for (final ArchiveNode child : children) {
			emitCompressed(child, nextParent, factory, gearType);
		}
	}

	private Optional<Object> resolveGear(final Artifact artifact) {
		if (artifact == null) {
			return Optional.empty();
		}
		return resolver.resolve(artifact);
	}
}