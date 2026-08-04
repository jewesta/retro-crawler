package com.retrocrawler.core.stash;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.gear.GearTreeFactory;

public final class StashFactory<G> implements GearTreeFactory<Stash<G>, StashFactory.MutableNode<G>, G> {

	private final Class<G> gearType;

	private final List<ArchiveBuild<G>> archives = new ArrayList<>();

	private ArchiveBuild<G> currentArchive;

	public StashFactory(final Class<G> gearType) {
		this.gearType = Objects.requireNonNull(gearType, "gearType");
	}

	@Override
	public Class<G> gearType() {
		return gearType;
	}

	@Override
	public void beginArchive(final ArchiveDescriptor archive) {
		Objects.requireNonNull(archive, "archive");
		if (currentArchive != null) {
			throw new IllegalStateException("beginArchive called while previous archive is still open.");
		}
		currentArchive = new ArchiveBuild<>(archive);
	}

	@Override
	public void endArchive(final ArchiveDescriptor archive) {
		Objects.requireNonNull(archive, "archive");
		if (currentArchive == null) {
			throw new IllegalStateException("endArchive called without a matching beginArchive.");
		}
		if (!currentArchive.archive.equals(archive)) {
			throw new IllegalStateException("endArchive called with a different archive than beginArchive.");
		}
		archives.add(currentArchive);
		currentArchive = null;
	}

	@Override
	public MutableNode<G> addNode(final MutableNode<G> parent, final G gear) {
		Objects.requireNonNull(gear, "gear");
		if (currentArchive == null) {
			throw new IllegalStateException("addNode called outside of beginArchive/endArchive. Expected: "
					+ GearTreeFactory.class.getSimpleName());
		}

		final MutableNode<G> node = new MutableNode<>(gear);

		if (parent == null) {
			currentArchive.roots.add(node);
		} else {
			parent.children.add(node);
		}

		return node;
	}

	@Override
	public Stash<G> build() {
		if (currentArchive != null) {
			throw new IllegalStateException("build called while an archive is still open.");
		}

		final List<ArchiveGear<G>> resultArchives = new ArrayList<>();
		for (final ArchiveBuild<G> archiveBuild : archives) {
			final List<GearNode<G>> roots = toImmutableNodes(archiveBuild.roots);
			resultArchives.add(new ArchiveGear<>(archiveBuild.archive, roots));
		}

		return new Stash<>(resultArchives);
	}

	private static <G> List<GearNode<G>> toImmutableNodes(final List<MutableNode<G>> nodes) {
		if (nodes.isEmpty()) {
			return List.of();
		}
		final List<GearNode<G>> out = new ArrayList<>(nodes.size());
		for (final MutableNode<G> node : nodes) {
			out.add(toImmutableNode(node));
		}
		return List.copyOf(out);
	}

	private static <G> GearNode<G> toImmutableNode(final MutableNode<G> node) {
		final List<GearNode<G>> children = toImmutableNodes(node.children);
		return new GearNode<>(node.gear, children);
	}

	private static final class ArchiveBuild<G> {

		private final ArchiveDescriptor archive;

		private final List<MutableNode<G>> roots = new ArrayList<>();

		private ArchiveBuild(final ArchiveDescriptor archive) {
			this.archive = archive;
		}
	}

	/**
	 * Internal mutable node used as the factory handle type.
	 */
	public static final class MutableNode<G> {

		private final G gear;

		private final List<MutableNode<G>> children = new ArrayList<>();

		private MutableNode(final G gear) {
			this.gear = gear;
		}
	}
}
