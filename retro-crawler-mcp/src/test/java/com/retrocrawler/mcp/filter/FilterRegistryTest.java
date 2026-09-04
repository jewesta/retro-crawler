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
		assertThat(registry.catalog().filters()).containsExactly(
				new FilterSummary("bus", "bus", FilterKind.CHOICES, true, List.of(FilterOperator.EQUALS),
						List.of("AGP")),
				new FilterSummary("released", "released", FilterKind.RANGE, false, List.of(FilterOperator.EQUALS),
						List.of()),
				new FilterSummary("title", "title", FilterKind.TEXT, false,
						List.of(FilterOperator.EQUALS, FilterOperator.CONTAINS), List.of()),
				new FilterSummary("edition", "edition", FilterKind.EXACT, false, List.of(FilterOperator.EQUALS),
						List.of()));
		assertThat(registry.key(choices)).isEqualTo("bus");
		assertThat(registry.definition("bus")).isSameAs(choices);
	}

	@Test
	void rejectsAmbiguousChoiceRepresentations() {
		final Object first = new DisplayValue("same");
		final Object second = new DisplayValue("same");
		final FilterDefinition<?> ambiguous = filter("choice", new FilterType.Choices<>(List.of(first, second)), false);

		assertThatIllegalStateException().isThrownBy(() -> new FilterRegistry("test_collection", List.of(ambiguous)))
				.withMessage("Filter 'choice' has several choices represented by MCP value 'same'.");
	}

	@Test
	void rejectsSeveralDefinitionsWithTheSameProtocolIdentity() {
		final FilterDefinition<?> first = filter("bus", new FilterType.Exact<>(), false);
		final FilterDefinition<?> second = filter("bus", new FilterType.Exact<>(), false);

		assertThatIllegalStateException()
				.isThrownBy(() -> new FilterRegistry("test_collection", List.of(first, second)))
				.withMessage("Several filter definitions map to MCP filter key 'bus'.");
	}

	@Test
	void rejectsUnknownProtocolIdsAndForeignDefinitions() {
		final FilterDefinition<?> registered = filter("bus", new FilterType.Exact<>(), false);
		final FilterDefinition<?> foreign = filter("title", new FilterType.Text(), false);
		final FilterRegistry registry = new FilterRegistry("test_collection", List.of(registered));

		assertThatIllegalArgumentException().isThrownBy(() -> registry.definition("title"))
				.withMessage("Unknown filter key: title");
		assertThatIllegalArgumentException().isThrownBy(() -> registry.key(foreign))
				.withMessage("Filter definition is not registered with this MCP server: title");
	}

	@SuppressWarnings({
			"rawtypes", "unchecked"
	})
	private static FilterDefinition<?> filter(final String key, final FilterType<?> type, final boolean multiple) {
		final FilterDefinition definition = mock(FilterDefinition.class);
		when(definition.key()).thenReturn(key);
		when(definition.name()).thenReturn(key);
		when(definition.filterType()).thenReturn(type);
		when(definition.multiple()).thenReturn(multiple);
		return definition;
	}

	private static final class DisplayValue {

		private final String text;

		private DisplayValue(final String text) {
			this.text = text;
		}

		@Override
		public String toString() {
			return text;
		}
	}
}
