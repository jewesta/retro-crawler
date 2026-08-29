package com.retrocrawler.core.stash;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.retrocrawler.core.gear.filter.FilterAvailability;
import com.retrocrawler.core.gear.filter.FilterDefinition;
import com.retrocrawler.core.gear.filter.FilterDefinitions;

/**
 * The complete immutable, model-dependent arrangement of resolved Gear.
 *
 * <p>
 * A stash is derived from the model-independent clue archives. It is a runtime
 * view rather than the collection's source of truth. Its gear stays grouped by
 * the archive it was found in, so every result keeps its provenance.
 */
public final class Stash implements FilterDefinitions {

	private final List<ArchiveGear<Object>> archives;

	private final FilterAvailabilityIndex filterAvailability;

	public Stash(final List<ArchiveGear<Object>> archives) {
		this(archives, List.of());
	}

	public Stash(final List<ArchiveGear<Object>> archives, final List<FilterDefinition<?>> filters) {
		this.archives = List.copyOf(Objects.requireNonNull(archives, "archives"));
		final List<GearNode<Object>> roots = new ArrayList<>();
		for (final ArchiveGear<Object> archive : this.archives) {
			roots.addAll(archive.roots());
		}
		filterAvailability = new FilterAvailabilityIndex(filters, roots);
	}

	/** Every archive and its natural Gear hierarchy, in composition order. */
	public List<ArchiveGear<Object>> archives() {
		return archives;
	}

	@Override
	public List<FilterDefinition<?>> filters() {
		return filterAvailability.filters();
	}

	/**
	 * Lazily computes this complete collection's availability for one filter.
	 */
	public <T> FilterAvailability<T> availability(final FilterDefinition<T> filter) {
		return filterAvailability.availability(filter);
	}

	/** Pulls every recognized Gear occurrence from this Stash. */
	public Batch<Object> pull() {
		return query(Object.class).pull();
	}

	/**
	 * Starts an immutable query selecting occurrences assignable to the type.
	 */
	public <G> Query<G> query(final Class<G> gearType) {
		return Query.from(this, gearType);
	}

}
