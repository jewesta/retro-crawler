package com.retrocrawler.mcp.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;

import com.retrocrawler.core.Journal;
import com.retrocrawler.core.RetroCrawler;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.gear.Fact;
import com.retrocrawler.core.gear.filter.FilterDefinition;
import com.retrocrawler.core.gear.filter.FilterType;
import com.retrocrawler.core.stash.ArchiveGear;
import com.retrocrawler.core.stash.Batch;
import com.retrocrawler.core.stash.GearNode;
import com.retrocrawler.core.stash.Query;
import com.retrocrawler.mcp.filter.FilterOperator;
import com.retrocrawler.mcp.filter.FilterRegistry;
import com.retrocrawler.mcp.filter.FilterValueText;

/**
 * Executes bounded MCP searches through RetroCrawler's public Stash query API.
 */
public final class GearSearch {

	public static final int DEFAULT_LIMIT = 25;

	public static final int MAXIMUM_LIMIT = 100;

	public static final int MAXIMUM_ARCHIVE_SELECTIONS = 100;

	public static final int MAXIMUM_CRITERIA = 20;

	private static final int MAXIMUM_FACTS = 100;

	private static final int MAXIMUM_VALUES = 50;

	private static final int MAXIMUM_VALUE_LENGTH = 1_000;

	private final RetroCrawler retroCrawler;

	private final FilterRegistry filters;

	public GearSearch(final RetroCrawler retroCrawler, final FilterRegistry filters) {
		this.retroCrawler = Objects.requireNonNull(retroCrawler, "retroCrawler");
		this.filters = Objects.requireNonNull(filters, "filters");
	}

	public SearchGearResult search(final List<String> archiveIds, final List<FilterCriterion> criteria,
			final Integer requestedOffset, final Integer requestedLimit) throws IOException {
		final int offset = offset(requestedOffset);
		final int limit = limit(requestedLimit);
		requireMaximumSize(archiveIds, MAXIMUM_ARCHIVE_SELECTIONS, "archiveIds");
		requireMaximumSize(criteria, MAXIMUM_CRITERIA, "criteria");
		Query<Object> query = retroCrawler.access(new Journal()).query(Object.class);
		if (archiveIds != null && !archiveIds.isEmpty()) {
			query = query.where(archiveIds.stream().map(GearSearch::archiveId).toList());
		}
		if (criteria != null) {
			for (final FilterCriterion criterion : criteria) {
				query = apply(query, Objects.requireNonNull(criterion, "criteria must not contain null"));
			}
		}

		final List<GearNode<Object>> matches = flatten(query.pull());
		final int from = Math.min(offset, matches.size());
		final int to = Math.min(from + limit, matches.size());
		final List<SearchGearHit> page = matches.subList(from, to).stream().map(this::summarize).toList();
		return new SearchGearResult(retroCrawler.collectionId(), matches.size(), offset, limit, to < matches.size(),
				page);
	}

	private Query<Object> apply(final Query<Object> query, final FilterCriterion criterion) {
		final FilterDefinition<?> filter = filters.definition(criterion.filterKey());
		if (criterion.operator() == FilterOperator.CONTAINS && !(filter.filterType() instanceof FilterType.Text)) {
			throw new IllegalArgumentException(
					"Filter '" + criterion.filterKey() + "' does not support operator CONTAINS.");
		}
		final Predicate<String> match = switch (criterion.operator()) {
		case EQUALS -> criterion.value()::equals;
		case CONTAINS -> caseInsensitiveContains(criterion.value());
		};
		return whereMatching(query, filter, match);
	}

	@SuppressWarnings({
			"rawtypes", "unchecked"
	})
	private static Query<Object> whereMatching(final Query<Object> query, final FilterDefinition<?> filter,
			final Predicate<String> match) {
		return query.whereMatching((FilterDefinition) filter, value -> match.test(FilterValueText.from(value)));
	}

	private SearchGearHit summarize(final GearNode<Object> node) {
		final List<Fact> availableFacts = node.trace().map(trace -> trace.resolved().facts()).orElseGet(List::of);
		final int factCount = Math.min(availableFacts.size(), MAXIMUM_FACTS);
		final List<SearchFact> facts = availableFacts.subList(0, factCount).stream().map(this::summarize).toList();
		final int issueCount = node.trace().map(trace -> trace.issues().size()).orElse(0);
		return new SearchGearHit(node.source().toString(), node.type(), facts, availableFacts.size() > MAXIMUM_FACTS,
				issueCount);
	}

	private SearchFact summarize(final Fact fact) {
		final FilterDefinition<?> definition = filters.definition(fact.key());
		final List<String> availableValues = fact.value().stream().map(FilterValueText::from).sorted().toList();
		final int valueCount = Math.min(availableValues.size(), MAXIMUM_VALUES);
		final List<String> values = availableValues.subList(0, valueCount).stream().map(GearSearch::abbreviate)
				.toList();
		return new SearchFact(fact.key(), definition.name(), values, fact.confidence().name(),
				availableValues.size() > MAXIMUM_VALUES);
	}

	private static List<GearNode<Object>> flatten(final Batch<Object> batch) {
		final List<GearNode<Object>> flattened = new ArrayList<>();
		for (final ArchiveGear<Object> archive : batch.archives()) {
			for (final GearNode<Object> root : archive.roots()) {
				flatten(root, flattened);
			}
		}
		return List.copyOf(flattened);
	}

	private static void flatten(final GearNode<Object> node, final List<GearNode<Object>> flattened) {
		flattened.add(node);
		for (final GearNode<Object> child : node.children()) {
			flatten(child, flattened);
		}
	}

	private static Predicate<String> caseInsensitiveContains(final String required) {
		final String normalized = required.toLowerCase(Locale.ROOT);
		return value -> value.toLowerCase(Locale.ROOT).contains(normalized);
	}

	private static ArchiveId archiveId(final String value) {
		return ArchiveId.of(Objects.requireNonNull(value, "archiveIds must not contain null"));
	}

	private static int offset(final Integer requested) {
		if (requested == null) {
			return 0;
		}
		if (requested < 0) {
			throw new IllegalArgumentException("offset must not be negative.");
		}
		return requested;
	}

	private static int limit(final Integer requested) {
		if (requested == null) {
			return DEFAULT_LIMIT;
		}
		if (requested < 1 || requested > MAXIMUM_LIMIT) {
			throw new IllegalArgumentException("limit must be between 1 and " + MAXIMUM_LIMIT + ".");
		}
		return requested;
	}

	private static String abbreviate(final String value) {
		return value.length() <= MAXIMUM_VALUE_LENGTH ? value : value.substring(0, MAXIMUM_VALUE_LENGTH - 1) + "…";
	}

	private static void requireMaximumSize(final List<?> values, final int maximum, final String name) {
		if (values != null && values.size() > maximum) {
			throw new IllegalArgumentException(name + " must not contain more than " + maximum + " entries.");
		}
	}
}
