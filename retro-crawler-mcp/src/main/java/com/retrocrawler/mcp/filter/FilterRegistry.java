package com.retrocrawler.mcp.filter;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.retrocrawler.core.gear.filter.FilterDefinition;

/**
 * Assigns stable protocol identities to one model's filter definitions.
 * Semantic Fact keys are already unique within a model and therefore serve as
 * the MCP identities without introducing a second naming scheme.
 */
public final class FilterRegistry {

	private final FilterCatalog catalog;

	private final Map<String, FilterDefinition<?>> definitionsById;

	private final Map<FilterDefinition<?>, String> idsByDefinition;

	public FilterRegistry(final String collectionId, final List<FilterDefinition<?>> definitions) {
		Objects.requireNonNull(definitions, "definitions");
		final Map<String, FilterDefinition<?>> mappedDefinitions = new LinkedHashMap<>();
		final Map<FilterDefinition<?>, String> mappedIds = new IdentityHashMap<>();
		final List<FilterSummary> summaries = definitions.stream()
				.map(definition -> map(definition, mappedDefinitions, mappedIds)).toList();
		this.catalog = new FilterCatalog(collectionId, summaries);
		this.definitionsById = Map.copyOf(mappedDefinitions);
		this.idsByDefinition = Collections.unmodifiableMap(new IdentityHashMap<>(mappedIds));
	}

	/** The protocol descriptions, in the model's deterministic filter order. */
	public FilterCatalog catalog() {
		return catalog;
	}

	/** Resolves an MCP filter identity to this model's exact definition. */
	public FilterDefinition<?> definition(final String id) {
		final String required = Objects.requireNonNull(id, "id");
		final FilterDefinition<?> definition = definitionsById.get(required);
		if (definition == null) {
			throw new IllegalArgumentException("Unknown filter id: " + required);
		}
		return definition;
	}

	/** Resolves one of this model's exact definitions to its MCP identity. */
	public String id(final FilterDefinition<?> definition) {
		final FilterDefinition<?> required = Objects.requireNonNull(definition, "definition");
		final String id = idsByDefinition.get(required);
		if (id == null) {
			throw new IllegalArgumentException(
					"Filter definition is not registered with this MCP server: " + required.key());
		}
		return id;
	}

	private static FilterSummary map(final FilterDefinition<?> definition,
			final Map<String, FilterDefinition<?>> definitionsById,
			final Map<FilterDefinition<?>, String> idsByDefinition) {
		final FilterDefinition<?> required = Objects.requireNonNull(definition, "definitions must not contain null");
		final String id = Objects.requireNonNull(required.key(), "filter key");
		if (id.isBlank()) {
			throw new IllegalStateException("A filter definition maps to a blank MCP filter id.");
		}
		final FilterDefinition<?> previous = definitionsById.putIfAbsent(id, required);
		if (previous != null) {
			throw new IllegalStateException("Several filter definitions map to MCP filter id '" + id + "'.");
		}
		idsByDefinition.put(required, id);
		return new FilterSummary(id, FilterKind.from(required.filterType()), required.multiple());
	}
}
