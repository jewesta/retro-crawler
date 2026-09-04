package com.retrocrawler.core.gear;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.retrocrawler.core.gear.filter.FilterDefinition;
import com.retrocrawler.core.gear.filter.FilterType;
import com.retrocrawler.core.util.Reflection;
import com.retrocrawler.core.util.RetroAttribute;
import com.retrocrawler.core.util.TypeName;

/**
 * Framework-owned reflective implementation of model-derived filter metadata.
 */
final class ReflectedFilterDefinition<T> implements FilterDefinition<T> {

	private final String key;
	private final String name;
	private final Class<T> valueType;
	private final boolean multiple;
	private final FilterType<T> filterType;
	private final Map<Class<?>, Field> bindings;
	private final List<Class<?>> gearTypes;

	private ReflectedFilterDefinition(final String key, final String name, final Class<T> valueType,
			final boolean multiple, final FilterType<T> filterType, final Map<Class<?>, Field> bindings) {
		this.key = requireKey(key);
		this.name = requireName(name);
		this.valueType = Objects.requireNonNull(valueType, "valueType");
		this.multiple = multiple;
		this.filterType = Objects.requireNonNull(filterType, "filterType");
		this.bindings = Map.copyOf(Objects.requireNonNull(bindings, "bindings"));
		if (bindings.isEmpty()) {
			throw new IllegalArgumentException("A filter requires at least one Gear binding: " + key);
		}
		this.gearTypes = List.copyOf(bindings.keySet());
		validateFilterType();
	}

	static FilterDefinition<?> create(final String key, final String name, final FactDescriptor descriptor,
			final FilterType<?> filterType, final Map<Class<?>, Field> bindings) {
		final Class<?> valueType = valueType(descriptor);
		final boolean multiple = Collection.class.isAssignableFrom(descriptor.field().getType())
				|| RetroAttribute.class.isAssignableFrom(descriptor.field().getType());
		return createTyped(key, name, valueType, multiple, filterType, bindings);
	}

	@SuppressWarnings({
			"unchecked", "rawtypes"
	})
	private static FilterDefinition<?> createTyped(final String key, final String name, final Class<?> valueType,
			final boolean multiple, final FilterType<?> filterType, final Map<Class<?>, Field> bindings) {
		return new ReflectedFilterDefinition(key, name, valueType, multiple, filterType, bindings);
	}

	@Override
	public String key() {
		return key;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public Class<T> valueType() {
		return valueType;
	}

	@Override
	public boolean multiple() {
		return multiple;
	}

	@Override
	public FilterType<T> filterType() {
		return filterType;
	}

	@Override
	public List<Class<?>> gearTypes() {
		return gearTypes;
	}

	@Override
	public boolean appliesTo(final Object gear) {
		return binding(Objects.requireNonNull(gear, "gear")) != null;
	}

	@Override
	public List<T> values(final Object gear) {
		Objects.requireNonNull(gear, "gear");
		final Field field = binding(gear);
		if (field == null) {
			return List.of();
		}

		final Object raw = read(field, gear);
		if (raw == null) {
			return List.of();
		}

		final Collection<?> values;
		if (raw instanceof final RetroAttribute attribute) {
			values = attribute.value();
		} else if (raw instanceof final Collection<?> collection) {
			values = collection;
		} else {
			values = List.of(raw);
		}

		final List<T> typed = new ArrayList<>(values.size());
		for (final Object value : values) {
			if (!valueType.isInstance(value)) {
				throw new IllegalStateException("Filter '" + key + "' expected values of type "
						+ TypeName.full(valueType) + " but Gear " + TypeName.full(gear.getClass()) + " contains "
						+ (value == null ? "null" : TypeName.full(value.getClass())) + ".");
			}
			typed.add(valueType.cast(value));
		}
		return List.copyOf(typed);
	}

	private Field binding(final Object gear) {
		final Class<?> actualType = gear.getClass();
		final Field exact = bindings.get(actualType);
		if (exact != null) {
			return exact;
		}
		for (final Map.Entry<Class<?>, Field> entry : bindings.entrySet()) {
			if (entry.getKey().isAssignableFrom(actualType)) {
				return entry.getValue();
			}
		}
		return null;
	}

	private static Object read(final Field field, final Object gear) {
		try {
			if (!field.canAccess(gear)) {
				field.setAccessible(true);
			}
			return field.get(gear);
		} catch (final IllegalAccessException e) {
			throw new IllegalStateException(
					"Cannot read filter field " + field + " from " + TypeName.full(gear.getClass()) + ".", e);
		}
	}

	private void validateFilterType() {
		if (filterType instanceof FilterType.Text && valueType != String.class && valueType != Object.class) {
			throw new IllegalArgumentException("Text filter '" + key + "' requires String values but is bound to "
					+ TypeName.full(valueType) + ".");
		}
		if (filterType instanceof final FilterType.Choices<?> choices) {
			for (final Object option : choices.options()) {
				if (!valueType.isInstance(option)) {
					throw new IllegalArgumentException(
							"Choice filter '" + key + "' declares " + TypeName.full(option.getClass()) + " option "
									+ option + " but its Fact value type is " + TypeName.full(valueType) + ".");
				}
			}
		}
	}

	private static String requireName(final String name) {
		final String required = Objects.requireNonNull(name, "name").strip();
		if (required.isEmpty()) {
			throw new IllegalArgumentException("A filter name must not be blank.");
		}
		return required;
	}

	private static Class<?> valueType(final FactDescriptor descriptor) {
		final Field field = descriptor.field();
		if (Collection.class.isAssignableFrom(field.getType())) {
			return descriptor.singleGenericArgument().orElse(Object.class);
		}
		if (RetroAttribute.class.isAssignableFrom(field.getType())) {
			return Object.class;
		}
		return Reflection.box(field.getType());
	}

	private static String requireKey(final String key) {
		final String normalized = Objects.requireNonNull(key, "key").trim();
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException("Filter key must not be blank.");
		}
		return normalized;
	}

}
