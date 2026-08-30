package com.retrocrawler.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.gear.filter.FilterDefinition;
import com.retrocrawler.core.gear.filter.FilterType;
import com.retrocrawler.core.stash.ArchiveGear;
import com.retrocrawler.core.stash.Batch;
import com.retrocrawler.core.stash.GearNode;
import com.retrocrawler.core.stash.Stash;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.TextField;

class FilterBarTest {

	private static final ArchiveId ARCHIVE_ID = ArchiveId.of("filter_bar_test");

	private static final TestFilter<String> TITLE = new TestFilter<>("title", "Title search", String.class,
			FilterType.text(), gear -> List.of(gear.title()));

	private static final TestFilter<Bus> BUS = new TestFilter<>("bus", "Expansion bus", Bus.class,
			FilterType.choices(List.of(Bus.values())), gear -> List.copyOf(gear.buses()));

	private static final TestFilter<Integer> RELEASED = new TestFilter<>("released", "Release year", Integer.class,
			FilterType.naturalRange(), gear -> gear.released() == null ? List.of() : List.of(gear.released()));

	private static final TestFilter<Edition> EDITION = new TestFilter<>("edition", "Edition", Edition.class,
			FilterType.exact(), gear -> List.of(gear.edition()));

	private static final TestFilter<String> UNUSED = new TestFilter<>("unused", "Unused", String.class,
			FilterType.text(), gear -> List.of());

	@Test
	void buildsRelevantControlsFromFilterMetadataAndAppliesThemImmediately() {
		final AtomicReference<Batch<TestGear>> result = new AtomicReference<>();
		final FilterBar<TestGear> bar = new FilterBar<>(TestGear.class, result::set);

		bar.setSource(stash(List.of(TITLE, BUS, RELEASED, EDITION, UNUSED)), ARCHIVE_ID);

		assertTrue(bar.isVisible());
		assertInstanceOf(TextField.class, component(bar, "filter-title"));
		final ComboBox<Bus> bus = comboBox(component(bar, "filter-bus"));
		assertInstanceOf(Component.class, component(bar, "filter-released"));
		assertInstanceOf(ComboBox.class, component(bar, "filter-released-minimum"));
		assertInstanceOf(ComboBox.class, component(bar, "filter-released-maximum"));
		assertInstanceOf(ComboBox.class, component(bar, "filter-edition"));
		assertFalse(hasComponent(bar, "filter-unused"));
		assertEquals("AGP (1)", bus.getItemLabelGenerator().apply(Bus.AGP));
		assertEquals("EISA (0)", bus.getItemLabelGenerator().apply(Bus.EISA));
		assertEquals(3, result.get().gear().size());

		final TextField title = (TextField) component(bar, "filter-title");
		assertEquals("Title search", title.getLabel());
		title.setValue("card");

		assertEquals(List.of("AGP card", "ISA card"), titles(result.get()));

		bus.setValue(Bus.AGP);

		assertEquals(List.of("AGP card"), titles(result.get()));

		bar.setSource(stash(List.of(TITLE, BUS, RELEASED, EDITION, UNUSED)), ARCHIVE_ID);
		final ComboBox<Integer> minimum = comboBox(component(bar, "filter-released-minimum"));
		assertEquals("Release year from", minimum.getLabel());
		minimum.setValue(1997);

		assertEquals(List.of("AGP card"), titles(result.get()));

		bar.setSource(stash(List.of(TITLE, BUS, RELEASED, EDITION, UNUSED)), ARCHIVE_ID);
		final ComboBox<Edition> edition = comboBox(component(bar, "filter-edition"));
		edition.setValue(new Edition("boxed"));

		assertEquals(List.of("ISA card"), titles(result.get()));
	}

	@Test
	void rebuildsItsFixedControlSetWhenTheAvailableStashChanges() {
		final AtomicReference<Batch<TestGear>> result = new AtomicReference<>();
		final FilterBar<TestGear> bar = new FilterBar<>(TestGear.class, result::set);
		bar.setSource(stash(List.of(TITLE, BUS)), ARCHIVE_ID);

		bar.setSource(stash(List.of(TITLE)), ARCHIVE_ID);

		assertTrue(hasComponent(bar, "filter-title"));
		assertFalse(hasComponent(bar, "filter-bus"));
		assertEquals(3, result.get().gear().size());
	}

	private static Stash stash(final List<FilterDefinition<?>> filters) {
		final List<TestGear> gear = List.of(new TestGear("AGP card", Set.of(Bus.AGP), 1997, new Edition("retail")),
				new TestGear("ISA card", Set.of(Bus.ISA), 1992, new Edition("boxed")),
				new TestGear("AGP manual", Set.of(), null, new Edition("print")));
		final List<GearNode<Object>> roots = gear.stream().map(value -> new GearNode<Object>(value,
				ARI.of("filter_bar", ARCHIVE_ID, Path.of(value.title())), List.of())).toList();
		return new Stash(List.of(new ArchiveGear<>(ArchiveDescriptor.of(ARCHIVE_ID, Path.of("archive")), roots)),
				filters);
	}

	private static List<String> titles(final Batch<TestGear> batch) {
		return batch.gear().stream().map(TestGear::title).toList();
	}

	@SuppressWarnings("unchecked")
	private static <T> ComboBox<T> comboBox(final Component component) {
		return (ComboBox<T>) assertInstanceOf(ComboBox.class, component);
	}

	private static boolean hasComponent(final Component root, final String id) {
		if (root.getId().filter(id::equals).isPresent()) {
			return true;
		}
		return root.getChildren().anyMatch(child -> hasComponent(child, id));
	}

	private static Component component(final Component root, final String id) {
		if (root.getId().filter(id::equals).isPresent()) {
			return root;
		}
		return root.getChildren().map(child -> findComponent(child, id)).flatMap(java.util.Optional::stream).findFirst()
				.orElseThrow();
	}

	private static java.util.Optional<Component> findComponent(final Component root, final String id) {
		if (root.getId().filter(id::equals).isPresent()) {
			return java.util.Optional.of(root);
		}
		return root.getChildren().map(child -> findComponent(child, id)).flatMap(java.util.Optional::stream)
				.findFirst();
	}

	private enum Bus {
		AGP,
		PCI,
		ISA,
		EISA
	}

	private record Edition(String name) {

		@Override
		public String toString() {
			return name;
		}
	}

	private record TestGear(String title, Set<Bus> buses, Integer released, Edition edition) {
	}

	private record TestFilter<T>(String key, String name, Class<T> valueType, FilterType<T> filterType,
			Function<TestGear, List<T>> values) implements FilterDefinition<T> {

		@Override
		public boolean multiple() {
			return false;
		}

		@Override
		public List<Class<?>> gearTypes() {
			return List.of(TestGear.class);
		}

		@Override
		public boolean appliesTo(final Object gear) {
			return gear instanceof TestGear;
		}

		@Override
		public List<T> values(final Object gear) {
			return gear instanceof final TestGear testGear ? values.apply(testGear) : List.of();
		}
	}
}
