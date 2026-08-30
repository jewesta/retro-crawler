package com.retrocrawler.mcp.filter;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.retrocrawler.core.gear.filter.FilterDefinition;
import com.retrocrawler.core.gear.filter.FilterType;

/**
 * Publishes stable protocol keys for one model's filter definitions. Semantic
 * Fact keys are already unique within a model and therefore serve as the MCP
 * keys without introducing a second naming scheme.
 */
public final class FilterRegistry {

	private final FilterCatalog catalog;

	private final Map<String, FilterDefinition<?>> definitionsByKey;

	private final Map<FilterDefinition<?>, String> keysByDefinition;

	public FilterRegistry(final String collectionId, final List<FilterDefinition<?>> definitions) {
		Objects.requireNonNull(definitions, "definitions");
		final Map<String, FilterDefinition<?>> mappedDefinitions = new LinkedHashMap<>();
		final Map<FilterDefinition<?>, String> mappedKeys = new IdentityHashMap<>();
		final List<FilterSummary> summaries = definitions.stream()
				.map(definition -> map(definition, mappedDefinitions, mappedKeys)).toList();
		this.catalog = new FilterCatalog(collectionId, summaries);
		this.definitionsByKey = Map.copyOf(mappedDefinitions);
		this.keysByDefinition = Collections.unmodifiableMap(new IdentityHashMap<>(mappedKeys));
	}

	/** The protocol descriptions, in the model's deterministic filter order. */
	public FilterCatalog catalog() {
		return catalog;
	}

	/** Resolves an MCP filter key to this model's exact definition. */
	public FilterDefinition<?> definition(final String key) {
		final String required = Objects.requireNonNull(key, "key");
		final FilterDefinition<?> definition = definitionsByKey.get(required);
		if (definition == null) {
			throw new IllegalArgumentException("Unknown filter key: " + required);
		}
		return definition;
	}

	/** Resolves one of this model's exact definitions to its MCP key. */
	public String key(final FilterDefinition<?> definition) {
		final FilterDefinition<?> required = Objects.requireNonNull(definition, "definition");
		final String key = keysByDefinition.get(required);
		if (key == null) {
			throw new IllegalArgumentException(
					"Filter definition is not registered with this MCP server: " + required.key());
		}
		return key;
	}

	private static FilterSummary map(final FilterDefinition<?> definition,
			final Map<String, FilterDefinition<?>> definitionsByKey,
			final Map<FilterDefinition<?>, String> keysByDefinition) {
		final FilterDefinition<?> required = Objects.requireNonNull(definition, "definitions must not contain null");
		final String key = Objects.requireNonNull(required.key(), "filter key");
		if (key.isBlank()) {
			throw new IllegalStateException("A filter definition maps to a blank MCP filter key.");
		}
		final FilterDefinition<?> previous = definitionsByKey.putIfAbsent(key, required);
		if (previous != null) {
			throw new IllegalStateException("Several filter definitions map to MCP filter key '" + key + "'.");
		}
		keysByDefinition.put(required, key);
		final FilterKind kind = FilterKind.from(required.filterType());
		return new FilterSummary(key, required.name(), kind, required.multiple(), operators(kind), choices(required));
	}

	private static List<FilterOperator> operators(final FilterKind kind) {
		return switch (kind) {
		case TEXT -> List.of(FilterOperator.EQUALS, FilterOperator.CONTAINS);
		case CHOICES, RANGE, EXACT -> List.of(FilterOperator.EQUALS);
		};
	}

	private static List<String> choices(final FilterDefinition<?> definition) {
		if (!(definition.filterType() instanceof final FilterType.Choices<?> choices)) {
			return List.of();
		}
		final LinkedHashSet<String> mapped = new LinkedHashSet<>();
		for (final Object choice : choices.options()) {
			final String value = FilterValueText.from(choice);
			if (value.isEmpty()) {
				throw new IllegalStateException("Filter '" + definition.key() + "' has a choice with no MCP value.");
			}
			if (!mapped.add(value)) {
				throw new IllegalStateException("Filter '" + definition.key()
						+ "' has several choices represented by MCP value '" + value + "'.");
			}
		}
		return List.copyOf(mapped);
	}
}
