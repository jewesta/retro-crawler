package com.retrocrawler.core.gear.injector;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.retrocrawler.core.util.Reflection;
import com.retrocrawler.core.util.RetroAttribute;
import com.retrocrawler.core.util.TypeName;

/**
 * Could be a static utility class. But the implementation might require more
 * dynamic code in the future. So we keep it "framework-like" non-static.
 */
final class RetroAttributeAdapter {

	Object adaptAttributeToField(final Class<?> gearType, final Field field, final String key,
			final RetroAttribute attribute) {

		final Class<?> fieldType = field.getType();

		/*
		 * Is the target field of type RetroAttribute? In that case the intent is for it
		 * to receive the attribute object itself (e.g. a Fact or a Clue), not its
		 * value.
		 */
		if (RetroAttribute.class.isAssignableFrom(fieldType)) {
			assertAssignableSingle(gearType, field, attribute, key);
			return attribute;
		}

		/*
		 * The target is an arbitrary type, not RetroAttribute. The intent is for it to
		 * receive the attribute value only. Since values can be single-valued or
		 * multi-valued, and may be of arbitrary element type, we need to adapt them to
		 * the declared field type if possible.
		 */
		final Set<? extends Object> values = requireNonEmpty(gearType, key, attribute.getValue());

		if (isCollectionTarget(field)) {
			return materializeCollectionForField(field, values);
		}

		/*
		 * Non-collection target: the attribute must be single-valued.
		 */
		if (values.size() != 1) {
			throw new IllegalStateException("Cannot assign multi-valued attribute for key '" + key
					+ "' to non-collection field " + field.getName() + " on gear " + TypeName.full(gearType)
					+ ". Use a collection field type or ensure the attribute has exactly one value.");
		}

		final Object single = values.iterator().next();
		assertAssignableSingle(gearType, field, single, key);
		return single;
	}

	private static Set<? extends Object> requireNonEmpty(final Class<?> gearType, final String key,
			final Set<? extends Object> values) {
		if (values == null || values.isEmpty()) {
			throw new IllegalStateException("Attribute for key '" + key + "' on gear " + TypeName.full(gearType)
					+ " is empty. This violates the attribute invariant.");
		}
		return values;
	}

	private static boolean isCollectionTarget(final Field field) {
		return Collection.class.isAssignableFrom(field.getType());
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

	private static void throwTypeMismatch(final Class<?> gearType, final Field field, final String key,
			final Class<?> valueType) {

		throw new IllegalStateException("Attribute type mismatch for key '" + key + "' on gear "
				+ TypeName.full(gearType) + " field " + field.getName() + ": expected " + TypeName.full(field.getType())
				+ " but got " + TypeName.full(valueType));
	}

	/**
	 * Materializes a collection value suitable for assignment to the given field.
	 * <p>
	 * Supported target field types:
	 * <ul>
	 * <li>{@link EnumSet}</li>
	 * <li>{@link List}</li>
	 * <li>{@link Set}</li>
	 * <li>{@link Collection}</li>
	 * <li>Concrete {@link Collection} implementations with a no-arg
	 * constructor</li>
	 * </ul>
	 * Other collection interfaces are rejected explicitly.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Collection<?> materializeCollectionForField(final Field field, final Set<? extends Object> values) {
		final Class<?> fieldType = field.getType();

		if (EnumSet.class.isAssignableFrom(fieldType)) {
			if (values.isEmpty()) {
				throw new IllegalStateException("Cannot assign empty values to EnumSet field " + field.getName()
						+ " because the enum type cannot be inferred.");
			}

			final Object first = values.iterator().next();
			if (!(first instanceof Enum)) {
				throw new IllegalStateException("Cannot assign non-enum values to EnumSet field " + field.getName()
						+ ". Expected enum values but got " + TypeName.full(first.getClass()) + ".");
			}

			final Class<? extends Enum> enumType = ((Enum) first).getDeclaringClass();
			final EnumSet set = EnumSet.noneOf(enumType);
			for (final Object value : values) {
				if (!(value instanceof final Enum enumValue) || enumValue.getDeclaringClass() != enumType) {
					throw new IllegalStateException("EnumSet field " + field.getName()
							+ " requires all values to be of the same enum type " + TypeName.full(enumType) + ".");
				}
				set.add(value);
			}

			return set;
		}

		/*
		 * If the target field type is an interface then we only support the three most
		 * common types: Collection, List and Set. Other collection interfaces (e.g.
		 * Queue, Deque, SortedSet) are rejected explicitly to avoid ambiguous
		 * semantics.
		 */
		if (fieldType.isInterface()) {
			if (List.class.isAssignableFrom(fieldType)) {
				return List.copyOf((Set) values);
			}
			if (Set.class.isAssignableFrom(fieldType)) {
				return Set.copyOf((Set) values);
			}
			if (Collection.class.equals(fieldType)) {
				return Set.copyOf((Set) values);
			}
			throw new IllegalStateException("Unsupported collection interface " + TypeName.full(fieldType)
					+ " on field " + field.getName() + ". Supported interfaces are List and Set.");
		}

		if (Reflection.newInstance(fieldType) instanceof final Collection collection) {
			collection.addAll(values);
			return collection;
		}
		throw new IllegalStateException("Field " + field.getName() + " is a concrete type " + TypeName.full(fieldType)
				+ " but is not a " + TypeName.full(Collection.class) + ".");

	}
}