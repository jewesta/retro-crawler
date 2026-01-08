package com.retrocrawler.core.gear;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.retrocrawler.core.annotation.RetroAnyAttribute;
import com.retrocrawler.core.annotation.RetroId;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.Confidence;
import com.retrocrawler.core.gear.matcher.GearMatcher;
import com.retrocrawler.core.util.Reflection;
import com.retrocrawler.core.util.RetroAttribute;
import com.retrocrawler.core.util.TypeName;

public class GearSpecialist implements GearMatcher, GearFactory {

	private final GearDescriptor definition;

	public GearSpecialist(final GearDescriptor descriptor) {
		this.definition = Objects.requireNonNull(descriptor, "descriptor");
	}

	public GearDescriptor getGearDefinition() {
		return definition;
	}

	@Override
	public Confidence matches(final GearContext context) {
		return definition.getMatcher().matches(context);
	}

	private static void assertMissingAllowed(final Class<?> gearType, final FactDescriptor attrDef, final String key) {

		final Field field = attrDef.getField();

		if (!attrDef.isOptional()) {
			throw new IllegalStateException(
					"Missing required attribute for key '" + key + "' on gear " + TypeName.full(gearType) + " field "
							+ field.getName() + ". Either provide a matching attribute or mark the field as optional.");
		}

		if (field.getType().isPrimitive()) {
			throw new IllegalStateException("Missing attribute for primitive field " + field.getName() + " (key '" + key
					+ "') on gear " + TypeName.full(gearType)
					+ ". Primitive fields cannot be null; make it non-primitive or provide the attribute.");
		}
	}

	private static void assertAssignableSingle(final Class<?> gearType, final Field field, final Object value,
			final String key) {

		final Class<?> fieldType = field.getType();
		final Class<?> valueType = value.getClass();

		if (fieldType.isPrimitive()) {
			final Class<?> boxed = Reflection.box(fieldType);
			if (!boxed.isAssignableFrom(valueType)) {
				throwTypeMismatch(gearType, field, key, valueType);
			}
			return;
		}

		if (!fieldType.isAssignableFrom(valueType)) {
			throwTypeMismatch(gearType, field, key, valueType);
		}
	}

	private static void assertAssignableCollection(final Class<?> gearType, final Field field, final Object value,
			final String key) {

		final Class<?> fieldType = field.getType();
		final Class<?> valueType = value.getClass();

		if (!fieldType.isAssignableFrom(valueType)) {
			throwTypeMismatch(gearType, field, key, valueType);
		}
	}

	private static void throwTypeMismatch(final Class<?> gearType, final Field field, final String key,
			final Class<?> valueType) {

		throw new IllegalStateException("Attribute type mismatch for key '" + key + "' on gear "
				+ TypeName.full(gearType) + " field " + field.getName() + ": expected " + TypeName.full(field.getType())
				+ " but got " + TypeName.full(valueType));
	}

	private static void setField(final Object target, final Field field, final Object value) {
		try {
			if (!field.canAccess(target)) {
				field.setAccessible(true);
			}
			field.set(target, value);
		} catch (final Exception e) {
			throw new IllegalStateException(
					"Cannot set field " + field.getName() + " on " + TypeName.full(target.getClass()), e);
		}
	}

	@SuppressWarnings("unchecked")
	private static void setAnyAttributeMap(final Object gear, final Field anyField,
			final Map<String, RetroAttribute> unassigned) {

		try {
			if (!anyField.canAccess(gear)) {
				anyField.setAccessible(true);
			}

			final Object current = anyField.get(gear);
			final Map<String, RetroAttribute> map;
			if (current == null) {
				map = new LinkedHashMap<>();
				anyField.set(gear, map);
			} else {
				map = (Map<String, RetroAttribute>) current;
			}

			map.putAll(unassigned);
		} catch (final Exception e) {
			throw new IllegalStateException("Cannot set " + TypeName.simple(RetroAnyAttribute.class) + " field "
					+ anyField.getName() + " on " + TypeName.full(gear.getClass()), e);
		}
	}

	private static boolean isCollectionTarget(final Field field) {
		return Collection.class.isAssignableFrom(field.getType());
	}

	private static Object adaptFactValueToField(final Class<?> gearType, final Field field, final String key,
			final Fact fact) {

		final Class<?> fieldType = field.getType();

		// Fact / RetroAttribute: inject the Fact object directly.
		if (fieldType.isAssignableFrom(Fact.class) || fieldType.isAssignableFrom(RetroAttribute.class)) {
			assertAssignableSingle(gearType, field, fact, key);
			return fact;
		}

		final Set<Object> values = fact.getValue();
		if (values.isEmpty()) {
			throw new IllegalStateException("Fact for key '" + key + "' on gear " + TypeName.full(gearType)
					+ " is empty. This violates the Fact invariant.");
		}

		// Set<T> / Collection<T>: inject values.
		if (isCollectionTarget(field)) {
			final Object collectionValue = materializeCollectionForField(field, values);
			assertAssignableCollection(gearType, field, collectionValue, key);
			return collectionValue;
		}

		// T: inject the single value (must be exactly 1).
		if (values.size() != 1) {
			throw new IllegalStateException("Cannot assign multi-valued fact for key '" + key + "' to non-collection "
					+ "field " + field.getName() + " on gear " + TypeName.full(gearType)
					+ ". Use a collection field type or ensure the fact has exactly one value.");
		}

		final Object single = values.iterator().next();
		assertAssignableSingle(gearType, field, single, key);
		return single;
	}

	private static Object adaptClueValueToField(final Class<?> gearType, final Field field, final String key,
			final Clue clue) {

		final Class<?> fieldType = field.getType();

		// Clue / RetroAttribute -> inject the Clue object directly.
		if (fieldType.isAssignableFrom(Clue.class) || fieldType.isAssignableFrom(RetroAttribute.class)) {
			assertAssignableSingle(gearType, field, clue, key);
			return clue;
		}

		final Set<String> values = clue.getValue();
		if (values == null || values.isEmpty()) {
			throw new IllegalStateException("Clue for key '" + key + "' on gear " + TypeName.full(gearType)
					+ " is empty. This violates the Clue invariant.");
		}

		// Set<String> / Collection<String>: inject values.
		if (isCollectionTarget(field)) {
			final Object collectionValue = materializeStringCollectionForField(field, values);
			assertAssignableCollection(gearType, field, collectionValue, key);
			return collectionValue;
		}

		// String: inject the single value (must be exactly 1).
		if (values.size() != 1) {
			throw new IllegalStateException("Cannot assign multi-valued clue for key '" + key + "' to non-collection "
					+ "field " + field.getName() + " on gear " + TypeName.full(gearType)
					+ ". Use a collection field type or ensure the clue has exactly one value.");
		}

		final String single = values.iterator().next();
		assertAssignableSingle(gearType, field, single, key);
		return single;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Object materializeCollectionForField(final Field field, final Set<Object> values) {
		final Class<?> fieldType = field.getType();

		if (EnumSet.class.isAssignableFrom(fieldType)) {
			if (values.isEmpty()) {
				return EnumSet.noneOf(Enum.class);
			}
			final Object first = values.iterator().next();
			if (!(first instanceof Enum)) {
				throw new IllegalStateException("Cannot assign non-enum values to EnumSet field " + field.getName()
						+ ". Expected enum values but got " + TypeName.full(first.getClass()) + ".");
			}
			final Class<? extends Enum> enumType = ((Enum) first).getDeclaringClass();
			final EnumSet set = EnumSet.noneOf(enumType);
			for (final Object v : values) {
				if (!(v instanceof Enum) || ((Enum) v).getDeclaringClass() != enumType) {
					throw new IllegalStateException("EnumSet field " + field.getName()
							+ " requires all values to be of the same enum type " + TypeName.full(enumType) + ".");
				}
				set.add(v);
			}
			return set;
		}

		if (fieldType.isInterface()) {
			if (List.class.isAssignableFrom(fieldType)) {
				return List.copyOf(values);
			}
			return Set.copyOf(values);
		}

		final Object instance = Reflection.newInstance(fieldType);
		if (!(instance instanceof Collection)) {
			throw new IllegalStateException("Field " + field.getName() + " is a concrete type "
					+ TypeName.full(fieldType) + " but is not a " + TypeName.full(Collection.class) + ".");
		}

		final Collection<Object> c = (Collection<Object>) instance;
		c.addAll(values);
		return instance;
	}

	@SuppressWarnings("unchecked")
	private static Object materializeStringCollectionForField(final Field field, final Set<String> values) {
		final Class<?> fieldType = field.getType();

		if (fieldType.isInterface()) {
			if (List.class.isAssignableFrom(fieldType)) {
				return List.copyOf(values);
			}
			return Set.copyOf(values);
		}

		final Object instance = Reflection.newInstance(fieldType);
		if (!(instance instanceof Collection)) {
			throw new IllegalStateException("Field " + field.getName() + " is a concrete type "
					+ TypeName.full(fieldType) + " but is not a " + TypeName.full(Collection.class) + ".");
		}

		final Collection<Object> c = (Collection<Object>) instance;
		c.addAll(values);
		return instance;
	}

	private static Object adaptAttributeValueToField(final Class<?> gearType, final Field field, final String key,
			final FactDescriptor attrDef, final RetroAttribute attribute) {

		if (!(attrDef instanceof FactDescriptor)) {
			throw new IllegalStateException(
					"Unknown " + TypeName.simple(FactDescriptor.class) + " type: " + TypeName.full(attrDef.getClass()));
		}

		if (!(attribute instanceof Fact)) {
			assertMissingAllowed(gearType, attrDef, key);
			return null;
		}

		return adaptFactValueToField(gearType, field, key, (Fact) attribute);
	}

	private static boolean isIdFieldAlreadyHandledByFact(final GearDescriptor definition, final Field idField) {
		for (final FactDescriptor def : definition.getAttributes().values()) {
			if (def.getField().equals(idField)) {
				return true;
			}
		}
		return false;
	}

	private static void injectStandaloneIdIfPresent(final GearDescriptor definition, final Class<?> gearType,
			final Object gear, final RetroAttributes attributes, final Map<String, RetroAttribute> unassigned) {

		Objects.requireNonNull(definition, "definition");
		Objects.requireNonNull(gearType, "gearType");
		Objects.requireNonNull(gear, "gear");
		Objects.requireNonNull(attributes, "attributes");
		Objects.requireNonNull(unassigned, "unassigned");

		final Optional<Field> idFieldOpt = definition.getIdField();
		if (idFieldOpt.isEmpty()) {
			return;
		}

		final Field idField = idFieldOpt.get();

		/*
		 * If the id field is also a @RetroFact field, it is already handled by the
		 * normal attribute injection loop.
		 */
		if (isIdFieldAlreadyHandledByFact(definition, idField)) {
			return;
		}

		final RetroAttribute attribute = attributes.get(Clue.KEY_INTERNAL_ID);
		if (attribute == null) {
			throw new IllegalStateException("Missing technical id clue for key '" + Clue.KEY_INTERNAL_ID
					+ "' required by standalone " + TypeName.simple(RetroId.class) + " on gear "
					+ TypeName.full(gearType) + " field " + idField.getName() + ".");
		}

		unassigned.remove(Clue.KEY_INTERNAL_ID);

		final Object injected;
		if (attribute instanceof Clue) {
			injected = adaptClueValueToField(gearType, idField, Clue.KEY_INTERNAL_ID, (Clue) attribute);
		} else if (attribute instanceof Fact) {
			injected = adaptFactValueToField(gearType, idField, Clue.KEY_INTERNAL_ID, (Fact) attribute);
		} else {
			throw new IllegalStateException("Technical id attribute for key '" + Clue.KEY_INTERNAL_ID + "' is neither "
					+ TypeName.full(Clue.class) + " nor " + TypeName.full(Fact.class) + " but "
					+ TypeName.full(attribute.getClass()) + ".");
		}

		if (injected != null) {
			setField(gear, idField, injected);
		}
	}

	@Override
	public Object create(final GearContext context) {
		Objects.requireNonNull(context, "context");

		final Class<?> type = definition.getType();
		final Object gear = Reflection.newInstance(type);

		final RetroAttributes attributes = context.attributes();
		Objects.requireNonNull(attributes, "attributes");

		final Map<String, RetroAttribute> unassigned = new LinkedHashMap<>();
		for (final Fact fact : attributes.getFacts()) {
			unassigned.put(fact.getKey(), fact);
		}
		for (final Clue clue : attributes.getClues()) {
			unassigned.put(clue.getKey(), clue);
		}

		for (final Entry<String, FactDescriptor> entry : definition.getAttributes().entrySet()) {
			final String key = entry.getKey();
			final FactDescriptor attrDef = entry.getValue();
			final Field field = attrDef.getField();

			final RetroAttribute attribute = attributes.get(key);
			unassigned.remove(key);

			if (attribute == null) {
				assertMissingAllowed(type, attrDef, key);
				continue;
			}

			final Object injected = adaptAttributeValueToField(type, field, key, attrDef, attribute);
			if (injected != null) {
				setField(gear, field, injected);
			}
		}

		injectStandaloneIdIfPresent(definition, type, gear, attributes, unassigned);

		definition.getAnyAttributeField().ifPresent(field -> {
			setAnyAttributeMap(gear, field, unassigned);
		});

		return gear;
	}
}