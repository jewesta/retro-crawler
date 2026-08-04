package com.retrocrawler.core.stash;

import java.util.List;
import java.util.Objects;

import com.retrocrawler.core.archive.ArchiveDescriptor;

/**
 * The resolved gear found in one archive, retaining the archive it came from.
 */
public final class ArchiveGear<G> {

	private final ArchiveDescriptor archive;

	private final List<GearNode<G>> roots;

	public ArchiveGear(final ArchiveDescriptor archive, final List<GearNode<G>> roots) {
		this.archive = Objects.requireNonNull(archive, "archive");
		this.roots = List.copyOf(Objects.requireNonNull(roots, "roots"));
	}

	public ArchiveDescriptor archive() {
		return archive;
	}

	public List<GearNode<G>> roots() {
		return roots;
	}
}
