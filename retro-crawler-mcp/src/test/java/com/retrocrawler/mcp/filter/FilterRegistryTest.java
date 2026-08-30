package com.retrocrawler.mcp.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.gear.filter.FilterDefinition;
import com.retrocrawler.core.gear.filter.FilterType;

class FilterRegistryTest {

	@Test
	void mapsEveryCoreFilterKindUnderItsSemanticFactKey() {
		final FilterDefinition<?> choices = filter("bus", new FilterType.Choices<>(List.of("AGP")), true);
		final FilterDefinition<?> range = filter("released", new FilterType.Range<Integer>(Comparator.naturalOrder()),
				false);
		final FilterDefinition<?> text = filter("title", new FilterType.Text(), false);
		final FilterDefinition<?> exact = filter("edition", new FilterType.Exact<>(), false);

		final FilterRegistry registry = new FilterRegistry("test_collection", List.of(choices, range, text, exact));

		assertThat(registry.catalog().collectionId()).isEqualTo("test_collection");
		assertThat(registry.catalog().filters()).containsExactly(new FilterSummary("bus", FilterKind.CHOICES, true),
				new FilterSummary("released", FilterKind.RANGE, false),
				new FilterSummary("title", FilterKind.TEXT, false),
				new FilterSummary("edition", FilterKind.EXACT, false));
		assertThat(registry.id(choices)).isEqualTo("bus");
		assertThat(registry.definition("bus")).isSameAs(choices);
	}

	@Test
	void rejectsSeveralDefinitionsWithTheSameProtocolIdentity() {
		final FilterDefinition<?> first = filter("bus", new FilterType.Exact<>(), false);
		final FilterDefinition<?> second = filter("bus", new FilterType.Exact<>(), false);

		assertThatIllegalStateException()
				.isThrownBy(() -> new FilterRegistry("test_collection", List.of(first, second)))
				.withMessage("Several filter definitions map to MCP filter id 'bus'.");
	}

	@Test
	void rejectsUnknownProtocolIdsAndForeignDefinitions() {
		final FilterDefinition<?> registered = filter("bus", new FilterType.Exact<>(), false);
		final FilterDefinition<?> foreign = filter("title", new FilterType.Text(), false);
		final FilterRegistry registry = new FilterRegistry("test_collection", List.of(registered));

		assertThatIllegalArgumentException().isThrownBy(() -> registry.definition("title"))
				.withMessage("Unknown filter id: title");
		assertThatIllegalArgumentException().isThrownBy(() -> registry.id(foreign))
				.withMessage("Filter definition is not registered with this MCP server: title");
	}

	@SuppressWarnings({
			"rawtypes", "unchecked"
	})
	private static FilterDefinition<?> filter(final String key, final FilterType<?> type, final boolean multiple) {
		final FilterDefinition definition = mock(FilterDefinition.class);
		when(definition.key()).thenReturn(key);
		when(definition.filterType()).thenReturn(type);
		when(definition.multiple()).thenReturn(multiple);
		return definition;
	}
}
