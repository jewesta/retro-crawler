package com.retrocrawler.core.gear;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.retrocrawler.core.annotation.RetroAnyAttribute;
import com.retrocrawler.core.annotation.RetroFact;
import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.core.annotation.RetroId;
import com.retrocrawler.core.gear.matcher.GearMatcher;
import com.retrocrawler.core.util.Descriptor;
import com.retrocrawler.core.util.Reflection;
import com.retrocrawler.core.util.TypeName;

public class GearDescriptor implements Descriptor {

	private final Class<?> type;

	private final GearMatcher matcher;

	private final Map<String, FactDescriptor> attributes;

	private final Field anyAttributeField;

	private final AnyAttributeMode anyAttributeMode;

	private final Field idField;

	private GearDescriptor(final Class<?> type, final GearMatcher matcher, final Map<String, FactDescriptor> attributes,
			final Field anyAttributeField, final Field idField) {
		this.type = Objects.requireNonNull(type, "type");
		this.matcher = Objects.requireNonNull(matcher, "matcher");
		this.attributes = Objects.requireNonNull(attributes, "attributes");
		this.anyAttributeField = anyAttributeField;
		this.anyAttributeMode = AnyAttributeMode.UNASSIGNED_ONLY;
		this.idField = idField;
	}

	public Class<?> getType() {
		return type;
	}

	public GearMatcher getMatcher() {
		return matcher;
	}

	public Map<String, FactDescriptor> getAttributes() {
		return attributes;
	}

	public Optional<Field> getAnyAttributeField() {
		return Optional.ofNullable(anyAttributeField);
	}

	public AnyAttributeMode getAnyAttributeMode() {
		return anyAttributeMode;
	}

	public Optional<Field> getIdField() {
		return Optional.ofNullable(idField);
	}

	public static Optional<GearDescriptor> of(final Class<?> type) {
		Objects.requireNonNull(type, "type");

		final RetroGear retroGear = type.getAnnotation(RetroGear.class);
		if (retroGear == null) {
			// No a retro gear
			return Optional.empty();
		}

		assertHasNoArgConstructor(type);

		final GearMatcher matcher = Reflection.newInstance(retroGear.value());

		final Map<String, FactDescriptor> attributes = new LinkedHashMap<>();
		Field anyAttributeField = null;
		Field idField = null;

		for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
			for (final Field field : c.getDeclaredFields()) {
				if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) {
					continue;
				}

				final RetroId retroId = field.getAnnotation(RetroId.class);
				if (retroId != null) {
					if (idField != null && !idField.equals(field)) {
						throw new IllegalArgumentException("Duplicate " + TypeName.simple(RetroId.class)
								+ ". Remove all but one: " + TypeName.full(type));
					}
					idField = field;
				}

				final RetroAnyAttribute anyAttr = field.getAnnotation(RetroAnyAttribute.class);
				if (anyAttr != null) {
					if (retroId != null) {
						throw new IllegalArgumentException(TypeName.simple(RetroId.class) + " must not be used on "
								+ TypeName.simple(RetroAnyAttribute.class) + " field: " + field + " in "
								+ TypeName.full(type));
					}
					if (anyAttributeField != null) {
						throw new IllegalArgumentException("Duplicate " + TypeName.simple(RetroAnyAttribute.class)
								+ ". Remove all but one: " + TypeName.full(type));
					}
					assertIsAnyAttributeMap(field, type);
					anyAttributeField = field;
				}

				final RetroFact fact = field.getAnnotation(RetroFact.class);

				if (retroId != null && fact != null && fact.optional()) {
					throw new IllegalArgumentException(TypeName.simple(RetroId.class) + " must not be used on optional "
							+ TypeName.simple(RetroFact.class) + " field: " + field + " in " + TypeName.full(type));
				}

				if (fact == null) {
					/*
					 * Standalone @RetroId is allowed, but it is not an attribute.
					 */
					continue;
				}

				final FactDescriptor incoming = new FactDescriptor(fact, field);

				final String key = incoming.getKey();
				final FactDescriptor existing = attributes.putIfAbsent(key, incoming);
				if (existing != null) {
					assertNonContradictingAttribute(existing.getField().getDeclaringClass(),
							incoming.getField().getDeclaringClass(), key, existing, incoming);
				}
			}
		}

		if (anyAttributeField == null && attributes.isEmpty()) {
			throw new IllegalArgumentException(TypeName.simple(RetroGear.class)
					+ " must have at least one field annotated with " + TypeName.simple(RetroAnyAttribute.class)
					+ " or " + TypeName.simple(RetroFact.class) + ": " + TypeName.full(type));
		}

		return Optional.of(new GearDescriptor(type, matcher, Map.copyOf(attributes), anyAttributeField, idField));
	}

	private static void assertHasNoArgConstructor(final Class<?> type) {
		try {
			final Constructor<?> ctor = type.getDeclaredConstructor();
			if (!ctor.canAccess(null)) {
				throw new IllegalArgumentException(TypeName.simple(RetroGear.class)
						+ " must have a public no-arg constructor: " + TypeName.full(type));
			}
		} catch (final NoSuchMethodException e) {
			throw new IllegalArgumentException(
					TypeName.simple(RetroGear.class) + " must have a no-arg constructor: " + TypeName.full(type), e);
		}
	}

	public static void assertNonContradictingAttribute(final Class<?> typeA, final Class<?> typeB, final String key,
			final FactDescriptor a, final FactDescriptor b) {

		Objects.requireNonNull(typeA, "typeA");
		Objects.requireNonNull(typeB, "typeB");
		Objects.requireNonNull(key, "key");
		Objects.requireNonNull(a, "a");
		Objects.requireNonNull(b, "b");

		if (!(a instanceof FactDescriptor) || !(b instanceof FactDescriptor)) {
			final String location = location(typeA, typeB);
			throw new IllegalStateException(
					"Unknown " + TypeName.simple(FactDescriptor.class) + " implementations for key '" + key + "' "
							+ location + ": " + TypeName.full(a.getClass()) + " and " + TypeName.full(b.getClass()));
		}

		final boolean sameOptional = a.isOptional() == b.isOptional();
		final boolean sameFieldType = a.getField().getType().equals(b.getField().getType());

		final Class<?> aGeneric = a.getSingleGenericArgument().orElse(null);
		final Class<?> bGeneric = b.getSingleGenericArgument().orElse(null);
		final boolean sameGenericType = Objects.equals(aGeneric, bGeneric);

		final FactDescriptor fa = a;
		final FactDescriptor fb = b;

		final boolean sameStrict = fa.isStrict() == fb.isStrict();
		final boolean sameParser = fa.getParser().equals(fb.getParser());

		if (sameOptional && sameFieldType && sameGenericType && sameStrict && sameParser) {
			return;
		}

		final String location = location(typeA, typeB);

		final StringBuilder details = new StringBuilder();
		details.append("optional=").append(fa.isOptional()).append(" vs ").append(fb.isOptional());
		details.append(", strict=").append(fa.isStrict()).append(" vs ").append(fb.isStrict());
		details.append(", parser=").append(TypeName.full(fa.getParser())).append(" vs ")
				.append(TypeName.full(fb.getParser()));
		details.append(", fieldType=").append(TypeName.full(fa.getField().getType())).append(" vs ")
				.append(TypeName.full(fb.getField().getType()));
		details.append(", genericType=").append(aGeneric == null ? "null" : TypeName.full(aGeneric)).append(" vs ")
				.append(bGeneric == null ? "null" : TypeName.full(bGeneric));

		throw new IllegalArgumentException("Contradicting " + TypeName.simple(RetroFact.class) + " for key '" + key
				+ "' (" + details + ") " + location);
	}

	private static String location(final Class<?> typeA, final Class<?> typeB) {
		if (typeA.equals(typeB)) {
			return "on " + TypeName.full(typeA);
		}
		return "between " + TypeName.full(typeA) + " and " + TypeName.full(typeB);
	}

	private static void assertIsAnyAttributeMap(final Field field, final Class<?> type) {
		if (!Map.class.isAssignableFrom(field.getType())) {
			throw new IllegalArgumentException(TypeName.simple(RetroAnyAttribute.class)
					+ " must be used on a Map field but is used on " + field + ": " + TypeName.full(type));
		}
		/*
		 * Note: Due to type erasure we cannot reliably enforce Map<String,
		 * RetroAttribute> at runtime. We at least ensure it is a Map and let the
		 * assignment logic validate key/value types.
		 */
	}
}