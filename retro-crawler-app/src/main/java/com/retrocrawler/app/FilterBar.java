package com.retrocrawler.app;

import static com.retrocrawler.core.gear.filter.FilterSelection.RELEVANT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.gear.filter.FilterAvailability;
import com.retrocrawler.core.gear.filter.FilterDefinition;
import com.retrocrawler.core.gear.filter.FilterType;
import com.retrocrawler.core.stash.Batch;
import com.retrocrawler.core.stash.Query;
import com.retrocrawler.core.stash.Stash;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.FlexLayout.FlexWrap;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;

/** A model-driven filter bar over one immutable Stash snapshot. */
final class FilterBar<G> extends VerticalLayout {

	private static final long serialVersionUID = -3479509244229843618L;

	private final Class<G> gearType;

	private final Consumer<Batch<G>> resultConsumer;

	private final FlexLayout controls = new FlexLayout();

	private final Span resultCount = new Span();

	private Stash stash;

	private ArchiveId archiveId;

	private List<FilterControl<G>> filterControls = List.of();

	FilterBar(final Class<G> gearType, final Consumer<Batch<G>> resultConsumer) {
		this.gearType = Objects.requireNonNull(gearType, "gearType");
		this.resultConsumer = Objects.requireNonNull(resultConsumer, "resultConsumer");

		addClassName("retro-filter-bar");
		setPadding(false);
		setSpacing(false);
		setWidthFull();

		final Span heading = new Span("Filter Gear");
		heading.addClassName("retro-filter-heading");
		resultCount.addClassName("retro-filter-result-count");
		final HorizontalLayout headingRow = new HorizontalLayout(heading, resultCount);
		headingRow.addClassName("retro-filter-heading-row");
		headingRow.setPadding(false);
		headingRow.setWidthFull();
		headingRow.setJustifyContentMode(JustifyContentMode.BETWEEN);

		controls.addClassName("retro-filter-controls");
		controls.setFlexWrap(FlexWrap.WRAP);
		controls.setWidthFull();
		add(headingRow, controls);
		setVisible(false);
	}

	void setSource(final Stash availableStash, final ArchiveId selectedArchiveId) {
		stash = Objects.requireNonNull(availableStash, "availableStash");
		archiveId = Objects.requireNonNull(selectedArchiveId, "selectedArchiveId");
		final Batch<G> available = baseQuery().pull();
		filterControls = available.filters(RELEVANT).stream().map(filter -> createControl(filter, available)).toList();
		controls.removeAll();
		filterControls.stream().map(FilterControl::component).forEach(controls::add);
		setVisible(!filterControls.isEmpty());
		publish(available);
	}

	void clear() {
		stash = null;
		archiveId = null;
		filterControls = List.of();
		controls.removeAll();
		resultCount.setText("");
		setVisible(false);
	}

	private void refresh() {
		if (stash == null) {
			return;
		}
		Query<G> query = baseQuery();
		for (final FilterControl<G> control : filterControls) {
			query = control.apply(query);
		}
		publish(query.pull());
	}

	private Query<G> baseQuery() {
		return stash.query(gearType).where(archiveId);
	}

	private void publish(final Batch<G> result) {
		resultCount.setText(result.gear().size() + " Gear");
		resultConsumer.accept(result);
	}

	@SuppressWarnings({
			"rawtypes", "unchecked"
	})
	private FilterControl<G> createControl(final FilterDefinition<?> filter, final Batch<G> available) {
		return createTypedControl((FilterDefinition) filter, available);
	}

	@SuppressWarnings("unchecked")
	private <T> FilterControl<G> createTypedControl(final FilterDefinition<T> filter, final Batch<G> available) {
		final FilterType<T> type = filter.filterType();
		if (type instanceof final FilterType.Choices<?> choices) {
			return choiceControl(filter, (FilterType.Choices<T>) choices, available);
		}
		if (type instanceof final FilterType.Range<?> range) {
			return rangeControl(filter, (FilterType.Range<T>) range, available);
		}
		if (type instanceof FilterType.Text) {
			return textControl((FilterDefinition<String>) filter);
		}
		if (type instanceof FilterType.Exact<?>) {
			return exactControl(filter, available);
		}
		throw new IllegalStateException("Unsupported FilterType: " + type.getClass().getName());
	}

	private <T> FilterControl<G> choiceControl(final FilterDefinition<T> filter, final FilterType.Choices<T> type,
			final Batch<G> available) {
		final ComboBox<T> input = comboBox(filter, "Any");
		final Map<T, Long> counts = choiceCounts(available, filter);
		input.setItems(type.options());
		input.setItemLabelGenerator(value -> display(value) + " (" + counts.getOrDefault(value, 0L) + ")");
		input.addValueChangeListener(event -> refresh());
		return new FilterControl<>(input) {

			@Override
			Query<G> apply(final Query<G> query) {
				final T selected = input.getValue();
				return selected == null ? query : query.where(filter, selected);
			}
		};
	}

	private FilterControl<G> textControl(final FilterDefinition<String> filter) {
		final TextField input = new TextField(filter.name());
		configure(input, filter);
		input.setPlaceholder("Contains...");
		input.setClearButtonVisible(true);
		input.setValueChangeMode(ValueChangeMode.EAGER);
		input.addValueChangeListener(event -> refresh());
		return new FilterControl<>(input) {

			@Override
			Query<G> apply(final Query<G> query) {
				final String required = input.getValue().strip().toLowerCase(Locale.ROOT);
				return required.isEmpty() ? query
						: query.whereMatching(filter, value -> value.toLowerCase(Locale.ROOT).contains(required));
			}
		};
	}

	private <T> FilterControl<G> exactControl(final FilterDefinition<T> filter, final Batch<G> available) {
		final ComboBox<T> input = comboBox(filter, "Any");
		input.setItems(observedValues(available, filter));
		input.setItemLabelGenerator(FilterBar::display);
		input.addValueChangeListener(event -> refresh());
		return new FilterControl<>(input) {

			@Override
			Query<G> apply(final Query<G> query) {
				final T selected = input.getValue();
				return selected == null ? query : query.where(filter, selected);
			}
		};
	}

	private <T> FilterControl<G> rangeControl(final FilterDefinition<T> filter, final FilterType.Range<T> type,
			final Batch<G> available) {
		final List<T> values = new ArrayList<>(observedValues(available, filter));
		values.sort(type.order());
		final ComboBox<T> minimum = comboBox(filter, "No minimum");
		final ComboBox<T> maximum = comboBox(filter, "No maximum");
		minimum.setLabel(filter.name() + " from");
		maximum.setLabel(filter.name() + " to");
		minimum.setId(id(filter) + "-minimum");
		maximum.setId(id(filter) + "-maximum");
		minimum.setItems(values);
		maximum.setItems(values);
		minimum.setItemLabelGenerator(FilterBar::display);
		maximum.setItemLabelGenerator(FilterBar::display);
		minimum.addValueChangeListener(event -> refresh());
		maximum.addValueChangeListener(event -> refresh());
		final HorizontalLayout range = new HorizontalLayout(minimum, maximum);
		range.addClassNames("retro-filter-control", "retro-filter-range");
		range.setPadding(false);
		range.setSpacing(true);
		range.setId(id(filter));
		return new FilterControl<>(range) {

			@Override
			Query<G> apply(final Query<G> query) {
				final T lower = minimum.getValue();
				final T upper = maximum.getValue();
				if (lower == null && upper == null) {
					return query;
				}
				return query.whereMatching(filter, value -> (lower == null || type.order().compare(value, lower) >= 0)
						&& (upper == null || type.order().compare(value, upper) <= 0));
			}
		};
	}

	private <T> ComboBox<T> comboBox(final FilterDefinition<T> filter, final String placeholder) {
		final ComboBox<T> input = new ComboBox<>(filter.name());
		configure(input, filter);
		input.setPlaceholder(placeholder);
		input.setClearButtonVisible(true);
		return input;
	}

	private static void configure(final Component input, final FilterDefinition<?> filter) {
		input.addClassName("retro-filter-control");
		input.setId(id(filter));
	}

	private static String id(final FilterDefinition<?> filter) {
		return "filter-" + filter.key().replaceAll("[^A-Za-z0-9_-]", "-");
	}

	private static String display(final Object value) {
		return String.valueOf(value);
	}

	private static <G, T> List<T> observedValues(final Batch<G> available, final FilterDefinition<T> filter) {
		final LinkedHashSet<T> values = new LinkedHashSet<>();
		for (final G gear : available.gear()) {
			values.addAll(filter.values(gear));
		}
		return List.copyOf(values);
	}

	@SuppressWarnings("unchecked")
	private static <G, T> Map<T, Long> choiceCounts(final Batch<G> available, final FilterDefinition<T> filter) {
		final FilterAvailability<T> availability = available.availability(filter);
		if (!(availability instanceof final FilterAvailability.Choices<?> choices)) {
			throw new IllegalArgumentException("Expected choice availability for filter: " + filter.key());
		}
		final Map<T, Long> counts = new HashMap<>();
		for (final FilterAvailability.Option<?> option : choices.options()) {
			counts.put((T) option.value(), option.matchingOccurrences());
		}
		return Map.copyOf(counts);
	}

	private abstract static class FilterControl<G> {

		private final Component component;

		FilterControl(final Component component) {
			this.component = Objects.requireNonNull(component, "component");
		}

		Component component() {
			return component;
		}

		abstract Query<G> apply(Query<G> query);
	}
}
