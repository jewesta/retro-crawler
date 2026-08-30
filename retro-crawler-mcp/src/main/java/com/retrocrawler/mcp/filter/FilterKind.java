package com.retrocrawler.mcp.filter;

import java.util.Objects;

import com.retrocrawler.core.gear.filter.FilterType;

/** The protocol-level filtering behavior of a model Fact. */
public enum FilterKind {

	CHOICES,

	RANGE,

	TEXT,

	EXACT;

	/**
	 * Since {@link FilterType} is sealed the compiler will complain here once a new type is added without also adding a {@link FilterKind}, protecting against forgotten mappings.
	 */
	static FilterKind from(final FilterType<?> type) {
		return switch (Objects.requireNonNull(type, "type")) {
		case final FilterType.Choices<?> ignored -> CHOICES;
		case final FilterType.Range<?> ignored -> RANGE;
		case final FilterType.Text ignored -> TEXT;
		case final FilterType.Exact<?> ignored -> EXACT;
		};
	}
}
