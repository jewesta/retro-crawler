package com.retrocrawler.core.stash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.Model;
import com.retrocrawler.core.RetroCrawler;
import com.retrocrawler.core.annotation.RetroClues;
import com.retrocrawler.core.annotation.RetroCollection;
import com.retrocrawler.core.annotation.RetroFact;
import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.archive.InMemoryRepository;
import com.retrocrawler.core.archive.clues.ArchiveFolderView;
import com.retrocrawler.core.archive.clues.ClueFinder;
import com.retrocrawler.core.archive.clues.Clues;
import com.retrocrawler.core.gear.Fact;
import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.filter.FilterAvailability;
import com.retrocrawler.core.gear.filter.FilterDefinition;
import com.retrocrawler.core.gear.filter.FilterType;
import com.retrocrawler.core.gear.matcher.AnyGearMatcher;
import com.retrocrawler.core.gear.parser.EnumFactParser;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.core.gear.parser.ParseContext;
import com.retrocrawler.core.gear.parser.StringParser;

class FactFilterTest {

	private static final ArchiveId ARCHIVE_ID = ArchiveId.of("collection");

	@Test
	void derivesOneStructuredFilterPerFactKeyFromTheEffectiveParsers() {
		final Model model = model();
		final FilterDefinition<Bus> bus = filter(model.filters(), "bus", Bus.class);
		final FilterDefinition<Edition> edition = filter(model.filters(), "edition", Edition.class);
		final FilterDefinition<Object> raw = filter(model.filters(), "raw", Object.class);
		final FilterDefinition<Integer> released = filter(model.filters(), "released", Integer.class);
		final FilterDefinition<String> title = filter(model.filters(), "title", String.class);
		final RetroCrawler crawler = RetroCrawler.builder().model(model).repository(new InMemoryRepository())
				.archive(archive()).build();

		assertEquals(model.filters(), crawler.filters());
		assertEquals(List.of("bus", "edition", "raw", "released", "title"),
				model.filters().stream().map(FilterDefinition::key).toList());
		assertTrue(bus.multiple());
		assertEquals(List.of(GraphicsCard.class, Motherboard.class), bus.gearTypes());
		assertEquals(List.of(Bus.values()), choices(bus.filterType()).options());
		assertFalse(released.multiple());
		assertInstanceOf(FilterType.Range.class, released.filterType());
		assertInstanceOf(FilterType.Text.class, title.filterType());
		assertInstanceOf(FilterType.Text.class, raw.filterType());
		assertInstanceOf(FilterType.Exact.class, edition.filterType());
	}

	@Test
	void reportsEveryConceivableChoiceAndItsObservedGearCount() {
		final Model model = model();
		final FilterDefinition<Bus> bus = filter(model.filters(), "bus", Bus.class);
		final FilterDefinition<Edition> edition = filter(model.filters(), "edition", Edition.class);
		final FilterDefinition<Integer> released = filter(model.filters(), "released", Integer.class);
		final FilterDefinition<String> title = filter(model.filters(), "title", String.class);
		final Stash stash = stash(model, new GraphicsCard(Set.of(Bus.AGP), 1997),
				new Motherboard(Set.of(Bus.PCI, Bus.ISA)), new GraphicsCard(Set.of(), null),
				new Magazine("Retro Hardware"));

		final FilterAvailability.Choices<Bus> availability = choices(stash.availability(bus));
		final FilterAvailability.Range<Integer> releasedAvailability = range(stash.availability(released));
		final FilterAvailability.Text titleAvailability = assertInstanceOf(FilterAvailability.Text.class,
				stash.availability(title));
		final FilterAvailability.Exact<Edition> editionAvailability = exact(stash.availability(edition));

		assertSame(availability, stash.availability(bus));
		assertEquals(2, availability.populatedOccurrences());
		assertEquals(
				List.of(new FilterAvailability.Option<>(Bus.AGP, 1), new FilterAvailability.Option<>(Bus.PCI, 1),
						new FilterAvailability.Option<>(Bus.ISA, 1), new FilterAvailability.Option<>(Bus.EISA, 0)),
				availability.options());
		assertTrue(availability.options().getFirst().present());
		assertFalse(availability.options().getLast().present());
		assertEquals(1997, releasedAvailability.minimum().orElseThrow());
		assertEquals(1997, releasedAvailability.maximum().orElseThrow());
		assertEquals(1, releasedAvailability.populatedOccurrences());
		assertEquals(1, titleAvailability.populatedOccurrences());
		assertEquals(1, editionAvailability.populatedOccurrences());
	}

	@Test
	void filtersEveryApplicableGearTypeWhileRetainingNonApplicableGear() {
		final Model model = model();
		final FilterDefinition<Bus> bus = filter(model.filters(), "bus", Bus.class);
		final GraphicsCard agpCard = new GraphicsCard(Set.of(Bus.AGP), 1997);
		final Magazine magazine = new Magazine("Retro Hardware");
		final Stash stash = stash(model, agpCard, new Motherboard(Set.of(Bus.PCI, Bus.ISA)),
				new GraphicsCard(Set.of(), null), magazine);

		final Batch<Object> result = stash.query(Object.class).where(bus, Bus.AGP).pull();
		final FilterAvailability.Choices<Bus> availability = choices(result.availability(bus));

		assertEquals(List.of(agpCard, magazine), result.gear());
		assertEquals(model.filters(), result.filters());
		assertEquals(1, availability.populatedOccurrences());
		assertEquals(List.of(1L, 0L, 0L, 0L),
				availability.options().stream().map(FilterAvailability.Option::matchingOccurrences).toList());
		assertSame(availability, result.availability(bus));
	}

	@Test
	void rejectsAFilterFromAnotherModelSnapshot() {
		final Model model = model();
		final FilterDefinition<Bus> foreign = filter(model().filters(), "bus", Bus.class);
		final Query<Object> query = stash(model, new GraphicsCard(Set.of(Bus.AGP), 1997)).query(Object.class);

		final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
				() -> query.where(foreign, Bus.AGP));

		assertEquals("Filter does not belong to this model: bus", failure.getMessage());
	}

	private static Model model() {
		return Model.from(Set.of(FilterCollection.class, GraphicsCard.class, Motherboard.class, Magazine.class,
				RawFactGear.class));
	}

	@SuppressWarnings("unchecked")
	private static <T> FilterType.Choices<T> choices(final FilterType<T> filterType) {
		return (FilterType.Choices<T>) assertInstanceOf(FilterType.Choices.class, filterType);
	}

	@SuppressWarnings("unchecked")
	private static <T> FilterAvailability.Choices<T> choices(final FilterAvailability<T> availability) {
		return (FilterAvailability.Choices<T>) assertInstanceOf(FilterAvailability.Choices.class, availability);
	}

	@SuppressWarnings("unchecked")
	private static <T> FilterAvailability.Range<T> range(final FilterAvailability<T> availability) {
		return (FilterAvailability.Range<T>) assertInstanceOf(FilterAvailability.Range.class, availability);
	}

	@SuppressWarnings("unchecked")
	private static <T> FilterAvailability.Exact<T> exact(final FilterAvailability<T> availability) {
		return (FilterAvailability.Exact<T>) assertInstanceOf(FilterAvailability.Exact.class, availability);
	}

	private static Stash stash(final Model model, final Object... gear) {
		final List<GearNode<Object>> roots = java.util.Arrays.stream(gear)
				.map(value -> new GearNode<>(value, source(value.getClass().getSimpleName()), List.of())).toList();
		return new Stash(List.of(new ArchiveGear<>(archive(), roots)), model.filters());
	}

	@SuppressWarnings("unchecked")
	private static <T> FilterDefinition<T> filter(final List<FilterDefinition<?>> filters, final String key,
			final Class<T> valueType) {
		final FilterDefinition<?> filter = filters.stream().filter(candidate -> candidate.key().equals(key)).findFirst()
				.orElseThrow();
		assertSame(valueType, filter.valueType());
		return (FilterDefinition<T>) filter;
	}

	private static ArchiveDescriptor archive() {
		return ArchiveDescriptor.of(ARCHIVE_ID, Path.of("collection"));
	}

	private static ARI source(final String name) {
		return ARI.of("filter_test", ARCHIVE_ID, Path.of(name));
	}

	private enum Bus {
		AGP,
		PCI,
		ISA,
		EISA
	}

	public record Edition(String name) {
	}

	@RetroCollection(id = "filter_test")
	@RetroClues(EmptyClueFinder.class)
	public static final class FilterCollection {
	}

	@RetroGear(AnyGearMatcher.class)
	public static final class GraphicsCard {

		@RetroFact(key = "bus", parser = BusParser.class)
		private Set<Bus> buses = Set.of();

		@RetroFact
		private Integer released;

		public GraphicsCard() {
		}

		private GraphicsCard(final Set<Bus> buses, final Integer released) {
			this.buses = Set.copyOf(buses);
			this.released = released;
		}
	}

	@RetroGear(AnyGearMatcher.class)
	public static final class Motherboard {

		@RetroFact(key = "bus", parser = BusParser.class)
		private Set<Bus> buses = Set.of();

		public Motherboard() {
		}

		private Motherboard(final Set<Bus> buses) {
			this.buses = Set.copyOf(buses);
		}
	}

	@RetroGear(AnyGearMatcher.class)
	public static final class Magazine {

		@RetroFact(parser = EditionParser.class)
		private Edition edition;

		@RetroFact
		private String title;

		public Magazine() {
		}

		private Magazine(final String title) {
			edition = new Edition("monthly");
			this.title = title;
		}
	}

	@RetroGear(AnyGearMatcher.class)
	public static final class RawFactGear {

		@RetroFact(parser = StringParser.class)
		private Fact raw;

		public RawFactGear() {
		}
	}

	public static final class BusParser implements EnumFactParser<Bus> {

		@Override
		public RatedFact<Bus> parse(final String rawValue, final ParseContext context) {
			return RatedFact.exact(Bus.valueOf(rawValue));
		}
	}

	public static final class EditionParser implements FactParser<Edition> {

		@Override
		public RatedFact<Edition> parse(final String rawValue, final ParseContext context) {
			return RatedFact.exact(new Edition(rawValue));
		}
	}

	public static final class EmptyClueFinder implements ClueFinder {

		@Override
		public Clues find(final ArchiveFolderView folder) {
			return Clues.none();
		}
	}
}
