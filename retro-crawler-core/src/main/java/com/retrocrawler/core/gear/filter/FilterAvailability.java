package com.retrocrawler.core.gear.filter;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Observed availability of one model filter within one immutable data set. */
public sealed interface FilterAvailability<T> permits FilterAvailability.Choices, FilterAvailability.Exact,
		FilterAvailability.Range, FilterAvailability.Text {

	FilterDefinition<T> filter();

	/** Gear occurrences to which the Fact applies and which carry a value. */
	long populatedOccurrences();

	/** Availability of one declared choice. */
	record Option<T>(T value, long matchingOccurrences) {

		public Option {
			Objects.requireNonNull(value, "value");
			if (matchingOccurrences < 0) {
				throw new IllegalArgumentException("matchingOccurrences must not be negative.");
			}
		}

		public boolean present() {
			return matchingOccurrences > 0;
		}
	}

	/** Declared choices with occurrence counts in model-defined order. */
	record Choices<T>(FilterDefinition<T> filter, List<Option<T>> options, long populatedOccurrences)
			implements FilterAvailability<T> {

		public Choices {
			Objects.requireNonNull(filter, "filter");
			options = List.copyOf(Objects.requireNonNull(options, "options"));
			requireNonNegative(populatedOccurrences);
		}
	}

	/** Observed bounds of an ordered filter. */
	record Range<T>(FilterDefinition<T> filter, Optional<T> minimum, Optional<T> maximum, long populatedOccurrences)
			implements FilterAvailability<T> {

		public Range {
			Objects.requireNonNull(filter, "filter");
			minimum = Objects.requireNonNull(minimum, "minimum");
			maximum = Objects.requireNonNull(maximum, "maximum");
			if (minimum.isPresent() != maximum.isPresent()) {
				throw new IllegalArgumentException(
						"Range minimum and maximum must either both be present or both be empty.");
			}
			requireNonNegative(populatedOccurrences);
		}
	}

	/** Population information for a textual filter. */
	record Text(FilterDefinition<String> filter, long populatedOccurrences) implements FilterAvailability<String> {

		public Text {
			Objects.requireNonNull(filter, "filter");
			requireNonNegative(populatedOccurrences);
		}
	}

	/** Population information for an exact-equality filter. */
	record Exact<T>(FilterDefinition<T> filter, long populatedOccurrences) implements FilterAvailability<T> {

		public Exact {
			Objects.requireNonNull(filter, "filter");
			requireNonNegative(populatedOccurrences);
		}
	}

	private static void requireNonNegative(final long populatedOccurrences) {
		if (populatedOccurrences < 0) {
			throw new IllegalArgumentException("populatedOccurrences must not be negative.");
		}
	}
}
