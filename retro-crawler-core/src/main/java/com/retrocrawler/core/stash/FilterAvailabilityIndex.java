package com.retrocrawler.core.stash;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.retrocrawler.core.gear.filter.FilterAvailability;
import com.retrocrawler.core.gear.filter.FilterDefinition;
import com.retrocrawler.core.gear.filter.FilterType;

/** Lazily computes and caches filter availability for one immutable forest. */
final class FilterAvailabilityIndex {

	private final List<FilterDefinition<?>> filters;
	private final List<GearNode<?>> roots;
	private final Map<FilterDefinition<?>, FilterAvailability<?>> cache = new IdentityHashMap<>();

	FilterAvailabilityIndex(final List<FilterDefinition<?>> filters, final List<? extends GearNode<?>> roots) {
		this.filters = List.copyOf(Objects.requireNonNull(filters, "filters"));
		this.roots = List.copyOf(Objects.requireNonNull(roots, "roots"));
	}

	List<FilterDefinition<?>> filters() {
		return filters;
	}

	@SuppressWarnings("unchecked")
	synchronized <T> FilterAvailability<T> availability(final FilterDefinition<T> filter) {
		Objects.requireNonNull(filter, "filter");
		if (filters.stream().noneMatch(candidate -> candidate == filter)) {
			throw new IllegalArgumentException("Filter does not belong to this model: " + filter.key());
		}
		return (FilterAvailability<T>) cache.computeIfAbsent(filter, this::calculate);
	}

	private FilterAvailability<?> calculate(final FilterDefinition<?> filter) {
		final FilterType<?> type = filter.filterType();
		if (type instanceof final FilterType.Choices<?> choices) {
			return choices(filter, choices);
		}
		if (type instanceof final FilterType.Range<?> range) {
			return range(filter, range);
		}
		if (type instanceof FilterType.Text) {
			return text(filter);
		}
		if (type instanceof FilterType.Exact<?>) {
			return exact(filter);
		}
		throw new IllegalStateException("Unknown FilterType: " + type.getClass().getName());
	}

	@SuppressWarnings({
			"unchecked", "rawtypes"
	})
	private FilterAvailability<?> choices(final FilterDefinition<?> rawFilter, final FilterType.Choices<?> rawChoices) {
		return calculateChoices((FilterDefinition) rawFilter, (FilterType.Choices) rawChoices);
	}

	private <T> FilterAvailability.Choices<T> calculateChoices(final FilterDefinition<T> filter,
			final FilterType.Choices<T> choices) {
		final Map<T, Long> counts = new LinkedHashMap<>();
		for (final T option : choices.options()) {
			counts.put(option, 0L);
		}

		final long[] populated = {
				0
		};
		visitGear(gear -> {
			if (!filter.appliesTo(gear)) {
				return;
			}
			final List<T> values = filter.values(gear);
			if (values.isEmpty()) {
				return;
			}
			populated[0]++;
			for (final T value : new LinkedHashSet<>(values)) {
				if (!counts.containsKey(value)) {
					throw new IllegalStateException(
							"Filter '" + filter.key() + "' produced undeclared choice: " + value);
				}
				counts.compute(value, (ignored, count) -> count + 1);
			}
		});

		final List<FilterAvailability.Option<T>> options = counts.entrySet().stream()
				.map(entry -> new FilterAvailability.Option<>(entry.getKey(), entry.getValue())).toList();
		return new FilterAvailability.Choices<>(filter, options, populated[0]);
	}

	@SuppressWarnings({
			"unchecked", "rawtypes"
	})
	private FilterAvailability<?> range(final FilterDefinition<?> rawFilter, final FilterType.Range<?> rawRange) {
		return calculateRange((FilterDefinition) rawFilter, (FilterType.Range) rawRange);
	}

	private <T> FilterAvailability.Range<T> calculateRange(final FilterDefinition<T> filter,
			final FilterType.Range<T> range) {
		final Comparator<? super T> order = range.order();
		final List<T> values = new ArrayList<>();
		final long[] populated = {
				0
		};
		visitGear(gear -> {
			if (!filter.appliesTo(gear)) {
				return;
			}
			final List<T> current = filter.values(gear);
			if (!current.isEmpty()) {
				populated[0]++;
				values.addAll(current);
			}
		});

		if (values.isEmpty()) {
			return new FilterAvailability.Range<>(filter, Optional.empty(), Optional.empty(), 0);
		}
		T minimum = values.getFirst();
		T maximum = minimum;
		for (final T value : values) {
			if (order.compare(value, minimum) < 0) {
				minimum = value;
			}
			if (order.compare(value, maximum) > 0) {
				maximum = value;
			}
		}
		return new FilterAvailability.Range<>(filter, Optional.of(minimum), Optional.of(maximum), populated[0]);
	}

	@SuppressWarnings("unchecked")
	private FilterAvailability.Text text(final FilterDefinition<?> filter) {
		return new FilterAvailability.Text((FilterDefinition<String>) filter, populated(filter));
	}

	private <T> FilterAvailability.Exact<T> exact(final FilterDefinition<T> filter) {
		return new FilterAvailability.Exact<>(filter, populated(filter));
	}

	private long populated(final FilterDefinition<?> filter) {
		final long[] count = {
				0
		};
		visitGear(gear -> {
			if (filter.appliesTo(gear) && !filter.values(gear).isEmpty()) {
				count[0]++;
			}
		});
		return count[0];
	}

	private void visitGear(final java.util.function.Consumer<Object> consumer) {
		for (final GearNode<?> root : roots) {
			visitGear(root, consumer);
		}
	}

	private static void visitGear(final GearNode<?> node, final java.util.function.Consumer<Object> consumer) {
		consumer.accept(node.gear());
		for (final GearNode<?> child : node.children()) {
			visitGear(child, consumer);
		}
	}
}
