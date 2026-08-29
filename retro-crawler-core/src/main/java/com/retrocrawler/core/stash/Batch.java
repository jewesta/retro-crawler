package com.retrocrawler.core.stash;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.retrocrawler.core.gear.filter.FilterAvailability;
import com.retrocrawler.core.gear.filter.FilterDefinition;
import com.retrocrawler.core.gear.filter.FilterDefinitions;

/**
 * An immutable, typed result pulled from a Stash.
 * <p>
 * The archive groups retain the lifted hierarchy of the matching Gear.
 * {@link #roots()} combines their roots into one forest, while the lazily
 * materialized {@link #gear()} view contains the same occurrences in pre-order.
 * Selection belongs to Query; a Batch only exposes its materialized result.
 */
public final class Batch<G> implements FilterDefinitions {

	private final List<ArchiveGear<G>> archives;

	private final List<GearNode<G>> roots;

	private volatile List<G> gear;

	private final FilterAvailabilityIndex filterAvailability;

	Batch(final List<ArchiveGear<G>> archives, final List<FilterDefinition<?>> filters) {
		this.archives = List.copyOf(Objects.requireNonNull(archives, "archives"));
		final List<GearNode<G>> accumulatedRoots = new ArrayList<>();
		for (final ArchiveGear<G> archive : this.archives) {
			accumulatedRoots.addAll(archive.roots());
		}
		this.roots = List.copyOf(accumulatedRoots);
		filterAvailability = new FilterAvailabilityIndex(filters, roots);
	}

	/** Matching Gear grouped into its archive-specific lifted hierarchies. */
	public List<ArchiveGear<G>> archives() {
		return archives;
	}

	/**
	 * All matching roots in archive and hierarchy order. Archive boundaries
	 * remain available through {@link #archives()} and every root's source ARI.
	 */
	public List<GearNode<G>> roots() {
		return roots;
	}

	@Override
	public List<FilterDefinition<?>> filters() {
		return filterAvailability.filters();
	}

	/** Lazily computes this result's availability for one model filter. */
	public <T> FilterAvailability<T> availability(final FilterDefinition<T> filter) {
		return filterAvailability.availability(filter);
	}

	/**
	 * Matching Gear in archive and hierarchy pre-order, flattened on first use.
	 */
	public List<G> gear() {
		List<G> available = gear;
		if (available == null) {
			synchronized (this) {
				available = gear;
				if (available == null) {
					final List<G> flattened = new ArrayList<>();
					for (final GearNode<G> root : roots) {
						flatten(root, flattened);
					}
					available = List.copyOf(flattened);
					gear = available;
				}
			}
		}
		return available;
	}

	private static <G> void flatten(final GearNode<G> node, final List<G> flattened) {
		flattened.add(node.gear());
		for (final GearNode<G> child : node.children()) {
			flatten(child, flattened);
		}
	}
}
