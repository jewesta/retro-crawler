package com.retrocrawler.core.stash;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.gear.filter.FilterDefinition;
import com.retrocrawler.core.gear.filter.FilterType;

/** An immutable, typed selection over one immutable Stash snapshot. */
public final class Query<G> {

	private final Stash stash;

	private final Class<G> gearType;

	private final Set<ArchiveId> archiveIds;

	private final List<Predicate<? super G>> predicates;

	private Query(final Stash stash, final Class<G> gearType, final Set<ArchiveId> archiveIds,
			final List<Predicate<? super G>> predicates) {
		this.stash = Objects.requireNonNull(stash, "stash");
		this.gearType = Objects.requireNonNull(gearType, "gearType");
		this.archiveIds = Set.copyOf(Objects.requireNonNull(archiveIds, "archiveIds"));
		this.predicates = List.copyOf(Objects.requireNonNull(predicates, "predicates"));
	}

	static <G> Query<G> from(final Stash stash, final Class<G> gearType) {
		Objects.requireNonNull(stash, "stash");
		final Set<ArchiveId> archiveIds = new LinkedHashSet<>();
		for (final ArchiveGear<Object> archive : stash.archives()) {
			archiveIds.add(archive.archive().id());
		}
		return new Query<>(stash, gearType, archiveIds, List.of());
	}

	/** Adds an archive criterion that limits this query to one archive. */
	public Query<G> where(final ArchiveId archiveId) {
		return where(List.of(Objects.requireNonNull(archiveId, "archiveId")));
	}

	/**
	 * Limits this query to the supplied archives. Repeated calls intersect
	 * their selections. Result ordering always follows the Stash's archive
	 * order.
	 */
	public Query<G> where(final Collection<ArchiveId> selectedArchiveIds) {
		Objects.requireNonNull(selectedArchiveIds, "selectedArchiveIds");
		final Set<ArchiveId> known = new LinkedHashSet<>();
		for (final ArchiveGear<Object> archive : stash.archives()) {
			known.add(archive.archive().id());
		}

		final Set<ArchiveId> selected = new LinkedHashSet<>();
		for (final ArchiveId archiveId : selectedArchiveIds) {
			final ArchiveId nonNullId = Objects.requireNonNull(archiveId, "selectedArchiveIds must not contain null");
			if (!known.contains(nonNullId)) {
				throw new IllegalArgumentException("Unknown archive: " + nonNullId);
			}
			if (archiveIds.contains(nonNullId)) {
				selected.add(nonNullId);
			}
		}
		return new Query<>(stash, gearType, selected, predicates);
	}

	/** Adds an in-process predicate that every returned Gear must satisfy. */
	public Query<G> where(final Predicate<? super G> predicate) {
		final List<Predicate<? super G>> selected = new ArrayList<>(predicates);
		selected.add(Objects.requireNonNull(predicate, "predicate"));
		return new Query<>(stash, gearType, archiveIds, selected);
	}

	/**
	 * Adds a predicate that applies only to Gear assignable to the supplied
	 * type. Gear of every other type remains selected.
	 */
	public <S extends G> Query<G> whereIf(final Class<S> selectedType, final Predicate<? super S> predicate) {
		final Class<S> nonNullType = Objects.requireNonNull(selectedType, "selectedType");
		final Predicate<? super S> nonNullPredicate = Objects.requireNonNull(predicate, "predicate");
		return where(gear -> !nonNullType.isInstance(gear) || nonNullPredicate.test(nonNullType.cast(gear)));
	}

	/**
	 * Requires at least one value of the supplied Fact to satisfy the predicate
	 * wherever that Fact applies. Gear types to which the Fact does not apply
	 * remain selected.
	 */
	public <T> Query<G> whereMatching(final FilterDefinition<T> filter, final Predicate<? super T> predicate) {
		final FilterDefinition<T> ownedFilter = requireOwnedFilter(filter);
		final Predicate<? super T> required = Objects.requireNonNull(predicate, "predicate");
		return where(gear -> !ownedFilter.appliesTo(gear) || ownedFilter.values(gear).stream().anyMatch(required));
	}

	/**
	 * Requires the supplied Fact value wherever that Fact applies. Gear types
	 * to which the Fact does not apply remain selected.
	 */
	public <T> Query<G> where(final FilterDefinition<T> filter, final T value) {
		final FilterDefinition<T> ownedFilter = requireOwnedFilter(filter);
		final T required = Objects.requireNonNull(value, "value");
		if (!ownedFilter.valueType().isInstance(required)) {
			throw new IllegalArgumentException("Filter '" + ownedFilter.key() + "' requires values of type "
					+ ownedFilter.valueType().getName() + " but got " + required.getClass().getName() + ".");
		}
		if (ownedFilter.filterType() instanceof final FilterType.Choices<?> choices
				&& !choices.options().contains(required)) {
			throw new IllegalArgumentException(
					"Filter '" + ownedFilter.key() + "' does not declare choice " + required + ".");
		}
		return whereMatching(ownedFilter, required::equals);
	}

	private <T> FilterDefinition<T> requireOwnedFilter(final FilterDefinition<T> filter) {
		final FilterDefinition<T> nonNullFilter = Objects.requireNonNull(filter, "filter");
		if (stash.filters().stream().noneMatch(candidate -> candidate == nonNullFilter)) {
			throw new IllegalArgumentException("Filter does not belong to this model: " + nonNullFilter.key());
		}
		return nonNullFilter;
	}

	/** Materializes this query as an immutable typed and lifted Batch. */
	public Batch<G> pull() {
		final List<ArchiveGear<G>> selectedArchives = new ArrayList<>();
		for (final ArchiveGear<Object> archive : stash.archives()) {
			if (!archiveIds.contains(archive.archive().id())) {
				continue;
			}
			selectedArchives.add(new ArchiveGear<>(archive.archive(), select(archive.roots())));
		}
		return new Batch<>(selectedArchives, stash.filters());
	}

	private List<GearNode<G>> select(final List<GearNode<Object>> nodes) {
		final List<GearNode<G>> selected = new ArrayList<>();
		for (final GearNode<Object> node : nodes) {
			selected.addAll(select(node));
		}
		return List.copyOf(selected);
	}

	private List<GearNode<G>> select(final GearNode<Object> node) {
		final List<GearNode<G>> selectedChildren = select(node.children());
		final Object gear = node.gear();
		if (!gearType.isInstance(gear)) {
			return selectedChildren;
		}

		final G typed = gearType.cast(gear);
		for (final Predicate<? super G> predicate : predicates) {
			if (!predicate.test(typed)) {
				return selectedChildren;
			}
		}
		return List.of(new GearNode<>(typed, node.source(), node.trace(), selectedChildren));
	}
}
