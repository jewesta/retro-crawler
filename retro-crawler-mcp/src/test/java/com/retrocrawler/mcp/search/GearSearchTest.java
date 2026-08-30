package com.retrocrawler.mcp.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.retrocrawler.core.RetroCrawler;
import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.archive.clues.Artifact;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.Clues;
import com.retrocrawler.core.gear.Confidence;
import com.retrocrawler.core.gear.Fact;
import com.retrocrawler.core.gear.filter.FilterDefinition;
import com.retrocrawler.core.gear.filter.FilterType;
import com.retrocrawler.core.gear.trace.ResolutionTrace;
import com.retrocrawler.core.stash.ArchiveGear;
import com.retrocrawler.core.stash.GearNode;
import com.retrocrawler.core.stash.Stash;
import com.retrocrawler.mcp.filter.FilterOperator;
import com.retrocrawler.mcp.filter.FilterRegistry;

class GearSearchTest {

	private static final String COLLECTION_ID = "test_collection";

	private static final ArchiveDescriptor HARDWARE = archive("hardware");

	private static final ArchiveDescriptor PRINT = archive("print");

	private FilterDefinition<String> bus;

	private FilterDefinition<String> title;

	private GearSearch search;

	@BeforeEach
	void setUp() throws IOException {
		bus = new TestFilter<>("bus", true, new FilterType.Choices<>(List.of("AGP", "PCI")), Card.class::isInstance,
				gear -> value(((Card) gear).bus()));
		title = new TestFilter<>("title", false, new FilterType.Text(), gear -> true,
				gear -> List.of(gear instanceof final Card card ? card.title() : ((Magazine) gear).title()));
		final List<FilterDefinition<?>> filters = List.of(bus, title);
		final Stash stash = new Stash(List.of(
				new ArchiveGear<>(HARDWARE,
						List.of(nodeWithFacts(new Card("AGP Wonder", "AGP"), HARDWARE, "agp", "bus", "AGP", "title",
								"AGP Wonder"), node(new Card("PCI Wonder", "PCI"), HARDWARE, "pci"),
								node(new Card("Unknown Bus", null), HARDWARE, "unknown"))),
				new ArchiveGear<>(PRINT, List.of(node(new Magazine("AGP Monthly"), PRINT, "monthly")))), filters);
		final RetroCrawler crawler = mock(RetroCrawler.class);
		when(crawler.collectionId()).thenReturn(COLLECTION_ID);
		when(crawler.filters()).thenReturn(filters);
		when(crawler.access(org.mockito.ArgumentMatchers.any())).thenReturn(stash);
		search = new GearSearch(crawler, new FilterRegistry(COLLECTION_ID, filters));
	}

	@Test
	void appliesStrictFilterCriteriaThroughTheStashQuery() throws IOException {
		final SearchGearResult result = search.search(List.of(),
				List.of(new FilterCriterion("bus", FilterOperator.EQUALS, "AGP")), null, null);

		assertThat(result.total()).isEqualTo(1);
		assertThat(result.offset()).isZero();
		assertThat(result.limit()).isEqualTo(GearSearch.DEFAULT_LIMIT);
		assertThat(result.hasMore()).isFalse();
		assertThat(result.gear()).extracting(SearchGearHit::source)
				.containsExactly("ari:/test_collection/hardware/agp");
		assertThat(result.gear().getFirst().facts()).containsExactly(
				new SearchFact("bus", List.of("AGP"), "EXACT", false),
				new SearchFact("title", List.of("AGP Wonder"), "EXACT", false));
	}

	@Test
	void supportsArchiveSelectionAndCaseInsensitiveTextContainment() throws IOException {
		final SearchGearResult result = search.search(List.of("print"),
				List.of(new FilterCriterion("title", FilterOperator.CONTAINS, "monthly")), 0, 10);

		assertThat(result.gear()).containsExactly(
				new SearchGearHit("ari:/test_collection/print/monthly", "Magazine", List.of(), false, 0));
	}

	@Test
	void returnsDeterministicBoundedPages() throws IOException {
		final SearchGearResult result = search.search(null, null, 1, 2);

		assertThat(result.total()).isEqualTo(4);
		assertThat(result.hasMore()).isTrue();
		assertThat(result.gear()).extracting(SearchGearHit::source).containsExactly("ari:/test_collection/hardware/pci",
				"ari:/test_collection/hardware/unknown");
	}

	@Test
	void rejectsUnknownFiltersUnsupportedOperatorsAndInvalidLimits() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> search.search(null,
						List.of(new FilterCriterion("missing", FilterOperator.EQUALS, "value")), null, null))
				.withMessage("Unknown filter id: missing");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> search.search(null,
						List.of(new FilterCriterion("bus", FilterOperator.CONTAINS, "AGP")), null, null))
				.withMessage("Filter 'bus' does not support operator CONTAINS.");
		assertThatIllegalArgumentException().isThrownBy(() -> search.search(null, null, null, 101))
				.withMessage("limit must be between 1 and 100.");
	}

	private static List<String> value(final String value) {
		return value == null ? List.of() : List.of(value);
	}

	private static ArchiveDescriptor archive(final String id) {
		return new ArchiveDescriptor(ArchiveId.of(id), id, Path.of(id));
	}

	private static GearNode<Object> node(final Object gear, final ArchiveDescriptor archive, final String path) {
		return new GearNode<>(gear, ARI.of(COLLECTION_ID, archive.id(), Path.of(path)), List.of());
	}

	private static GearNode<Object> nodeWithFacts(final Object gear, final ArchiveDescriptor archive, final String path,
			final String firstKey, final String firstValue, final String secondKey, final String secondValue) {
		final ARI source = ARI.of(COLLECTION_ID, archive.id(), Path.of(path));
		final Clue firstClue = Clue.of(firstKey, firstValue);
		final Clue secondClue = Clue.of(secondKey, secondValue);
		final List<Fact> facts = List.of(new Fact(firstKey, Set.of(firstValue), Confidence.EXACT, firstClue),
				new Fact(secondKey, Set.of(secondValue), Confidence.EXACT, secondClue));
		final ResolutionTrace.Attributes attributes = new ResolutionTrace.Attributes(facts, Clues.none());
		final ResolutionTrace.Match match = new ResolutionTrace.Match(gear.getClass(), GearSearchTest.class,
				Confidence.EXACT);
		final ResolutionTrace trace = new ResolutionTrace(new Artifact(Clues.of(firstClue, secondClue)), attributes,
				List.of(match), match, attributes, List.of());
		return new GearNode<>(gear, source, trace, List.of());
	}

	private record Card(String title, String bus) {
	}

	private record Magazine(String title) {
	}

	private record TestFilter<T>(String key, boolean multiple, FilterType<T> filterType, Predicate<Object> applies,
			Function<Object, List<T>> values) implements FilterDefinition<T> {

		@Override
		public Class<T> valueType() {
			@SuppressWarnings("unchecked")
			final Class<T> type = (Class<T>) String.class;
			return type;
		}

		@Override
		public List<Class<?>> gearTypes() {
			return List.of();
		}

		@Override
		public boolean appliesTo(final Object gear) {
			return applies.test(gear);
		}

		@Override
		public List<T> values(final Object gear) {
			return appliesTo(gear) ? values.apply(gear) : List.of();
		}
	}
}
