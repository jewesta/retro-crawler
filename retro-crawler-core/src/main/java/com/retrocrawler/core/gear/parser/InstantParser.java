package com.retrocrawler.core.gear.parser;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.Objects;
import java.util.regex.Pattern;

import com.retrocrawler.core.gear.RatedFact;

/**
 * Parses exact points in time from ISO offset timestamps or epoch milliseconds.
 */
public final class InstantParser implements OrderedFactParser<Instant> {

	private static final Pattern INTEGER = Pattern.compile("[+-]?\\d+");

	@Override
	public Comparator<? super Instant> order() {
		return Comparator.naturalOrder();
	}

	@Override
	public RatedFact<Instant> parse(final String rawValue, final ParseContext context) {
		Objects.requireNonNull(context, "context");
		if (rawValue == null || rawValue.isBlank()) {
			return RatedFact.none("Expected an ISO timestamp with an offset or epoch milliseconds.");
		}

		final String value = rawValue.trim();
		try {
			return RatedFact.exact(Instant.parse(value));
		} catch (final DateTimeParseException ignored) {
			// Try the wider ISO offset date-time representation next.
		}
		try {
			return RatedFact.exact(OffsetDateTime.parse(value).toInstant());
		} catch (final DateTimeParseException ignored) {
			// An integer is explicitly interpreted as epoch milliseconds.
		}
		if (INTEGER.matcher(value).matches()) {
			try {
				return RatedFact.exact(Instant.ofEpochMilli(Long.parseLong(value)));
			} catch (final NumberFormatException ignored) {
				// Fall through to the common rejection below.
			}
		}
		return RatedFact.none("Expected an ISO timestamp with an offset or epoch milliseconds but got: " + rawValue);
	}
}
