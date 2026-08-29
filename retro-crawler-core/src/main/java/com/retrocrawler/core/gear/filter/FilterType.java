package com.retrocrawler.core.gear.filter;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * Describes the structured filtering semantics of one parsed Fact value type.
 * It is model metadata rather than an applied query criterion or UI widget.
 */
public sealed interface FilterType<T> permits FilterType.Choices, FilterType.Exact, FilterType.Range, FilterType.Text {

	/** A closed, ordered set of conceivable values. */
	record Choices<T>(List<T> options) implements FilterType<T> {

		public Choices {
			Objects.requireNonNull(options, "options");
			final LinkedHashSet<T> unique = new LinkedHashSet<>();
			for (final T option : options) {
				if (!unique.add(Objects.requireNonNull(option, "options must not contain null"))) {
					throw new IllegalArgumentException("Filter choices must not contain duplicates: " + option);
				}
			}
			if (unique.isEmpty()) {
				throw new IllegalArgumentException("A choice filter requires at least one option.");
			}
			options = List.copyOf(unique);
		}
	}

	/** An ordered value supporting lower and upper bounds. */
	record Range<T>(Comparator<? super T> order) implements FilterType<T> {

		public Range {
			Objects.requireNonNull(order, "order");
		}
	}

	/** A textual value supporting text matching. */
	record Text() implements FilterType<String> {
	}

	/** A value supporting exact equality. */
	record Exact<T>() implements FilterType<T> {
	}

	static <T> Choices<T> choices(final Collection<? extends T> options) {
		return new Choices<>(List.copyOf(Objects.requireNonNull(options, "options")));
	}

	static <T> Range<T> range(final Comparator<? super T> order) {
		return new Range<>(order);
	}

	static <T extends Comparable<? super T>> Range<T> naturalRange() {
		return range(Comparator.naturalOrder());
	}

	static Text text() {
		return new Text();
	}

	static <T> Exact<T> exact() {
		return new Exact<>();
	}
}
