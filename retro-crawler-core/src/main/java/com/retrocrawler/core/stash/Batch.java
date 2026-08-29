package com.retrocrawler.core.stash;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An immutable, typed result pulled from a Stash.
 * <p>
 * The archive groups retain the lifted hierarchy of the matching Gear. The flat
 * {@link #gear()} view contains the same occurrences in pre-order.
 */
public final class Batch<G> {

	private final List<ArchiveGear<G>> archives;

	private final List<G> gear;

	Batch(final List<ArchiveGear<G>> archives) {
		this.archives = List.copyOf(Objects.requireNonNull(archives, "archives"));
		final List<G> flattened = new ArrayList<>();
		for (final ArchiveGear<G> archive : archives) {
			for (final GearNode<G> root : archive.roots()) {
				flatten(root, flattened);
			}
		}
		this.gear = List.copyOf(flattened);
	}

	/** Matching Gear grouped into its archive-specific lifted hierarchies. */
	public List<ArchiveGear<G>> archives() {
		return archives;
	}

	/** Matching Gear in archive and hierarchy pre-order. */
	public List<G> gear() {
		return gear;
	}

	private static <G> void flatten(final GearNode<G> node, final List<G> flattened) {
		flattened.add(node.gear());
		for (final GearNode<G> child : node.children()) {
			flatten(child, flattened);
		}
	}
}
