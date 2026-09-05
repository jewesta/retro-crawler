package com.retrocrawler.core.gear;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.retrocrawler.core.annotation.RetroAnyAttribute;
import com.retrocrawler.core.annotation.RetroAnyGear;
import com.retrocrawler.core.annotation.RetroFact;
import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.core.annotation.RetroId;
import com.retrocrawler.core.annotation.RetroSource;
import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.archive.clues.InternalClueKeys;
import com.retrocrawler.core.gear.matcher.GearMatcher;
import com.retrocrawler.core.util.Descriptor;
import com.retrocrawler.core.util.Reflection;
import com.retrocrawler.core.util.TypeName;

public class GearDescriptor implements Descriptor {

	private final Class<?> type;

	private final GearType gearType;

	private final GearMatcher matcher;

	private final Map<String, FactDescriptor> attributes;

	private final List<FactDescriptor> factDeclarations;

	private final Field anyAttributeField;

	private final AnyAttributeMode anyAttributeMode;

	private final Field idField;

	private final Field sourceField;

	private GearDescriptor(final Class<?> type, final GearType gearType, final GearMatcher matcher,
			final Map<String, FactDescriptor> attributes, final List<FactDescriptor> factDeclarations,
			final Field anyAttributeField, final Field idField, final Field sourceField) {
		this.type = Objects.requireNonNull(type, "type");
		this.gearType = Objects.requireNonNull(gearType, "gearType");
		this.matcher = matcher;
		this.attributes = Objects.requireNonNull(attributes, "attributes");
		this.factDeclarations = List.copyOf(Objects.requireNonNull(factDeclarations, "factDeclarations"));
		this.anyAttributeField = anyAttributeField;
		this.anyAttributeMode = AnyAttributeMode.UNASSIGNED_ONLY;
		this.idField = idField;
		this.sourceField = sourceField;
	}

	public Class<?> type() {
		return type;
	}

	public GearType gearType() {
		return gearType;
	}

	public Optional<GearMatcher> matcher() {
		return Optional.ofNullable(matcher);
	}

	public boolean isAnyGear() {
		return matcher == null;
	}

	public Map<String, FactDescriptor> attributes() {
		return attributes;
	}

	List<FactDescriptor> factDeclarations() {
		return factDeclarations;
	}

	public Optional<Field> anyAttributeField() {
		return Optional.ofNullable(anyAttributeField);
	}

	public AnyAttributeMode anyAttributeMode() {
		return anyAttributeMode;
	}

	public Optional<Field> idField() {
		return Optional.ofNullable(idField);
	}

	public Optional<Field> sourceField() {
		return Optional.ofNullable(sourceField);
	}

	public Optional<String> idAttributeKey() {
		if (idField == null) {
			return Optional.empty();
		}
		for (final Map.Entry<String, FactDescriptor> entry : attributes.entrySet()) {
			if (entry.getValue().field().equals(idField)) {
				return Optional.of(entry.getKey());
			}
		}
		return Optional.of(InternalClueKeys.ID);
	}

	public static Optional<GearDescriptor> of(final Class<?> type) {
		Objects.requireNonNull(type, "type");

		final RetroGear retroGear = type.getAnnotation(RetroGear.class);
		final RetroAnyGear anyGear = type.getAnnotation(RetroAnyGear.class);
		if (retroGear == null && anyGear == null) {
			// Not a Gear declaration
			return Optional.empty();
		}
		if (retroGear != null && anyGear != null) {
			throw new IllegalArgumentException(
					TypeName.simple(RetroGear.class) + " and " + TypeName.simple(RetroAnyGear.class)
							+ " must not both annotate Gear type " + TypeName.full(type) + ".");
		}

		assertHasNoArgConstructor(type);

		final GearMatcher matcher = retroGear == null ? null : Reflection.newInstance(retroGear.value());

		final Map<String, FactDescriptor> attributes = new LinkedHashMap<>();
		final List<FactDescriptor> factDeclarations = new ArrayList<>();
		Field anyAttributeField = null;
		Field idField = null;
		Field sourceField = null;

		for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
			for (final Field field : c.getDeclaredFields()) {
				if (field.isSynthetic()) {
					continue;
				}

				final RetroSource retroSource = field.getAnnotation(RetroSource.class);
				if (Modifier.isStatic(field.getModifiers())) {
					if (retroSource != null) {
						throw new IllegalArgumentException(TypeName.simple(RetroSource.class)
								+ " must not be used on a static field: " + field + " in " + TypeName.full(type));
					}
					continue;
				}
				if (retroSource != null) {
					if (sourceField != null) {
						throw new IllegalArgumentException("Duplicate " + TypeName.simple(RetroSource.class)
								+ ". Remove all but one: " + TypeName.full(type));
					}
					assertIsSourceAri(field, type);
					sourceField = field;
				}

				final RetroId retroId = field.getAnnotation(RetroId.class);
				if (retroId != null) {
					if (retroSource != null) {
						throw conflictingAnnotations(RetroSource.class, RetroId.class, field, type);
					}
					if (idField != null && !idField.equals(field)) {
						throw new IllegalArgumentException("Duplicate " + TypeName.simple(RetroId.class)
								+ ". Remove all but one: " + TypeName.full(type));
					}
					idField = field;
				}

				final RetroAnyAttribute anyAttr = field.getAnnotation(RetroAnyAttribute.class);
				if (anyAttr != null) {
					if (retroSource != null) {
						throw conflictingAnnotations(RetroSource.class, RetroAnyAttribute.class, field, type);
					}
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
				if (fact != null && retroSource != null) {
					throw conflictingAnnotations(RetroSource.class, RetroFact.class, field, type);
				}

				if (fact == null) {
					/*
					 * Standalone @RetroId is allowed, but it is not an
					 * attribute.
					 */
					continue;
				}

				final FactDescriptor incoming = new FactDescriptor(fact, field);
				factDeclarations.add(incoming);

				final String key = incoming.key();
				final FactDescriptor existing = attributes.putIfAbsent(key, incoming);
				if (existing != null) {
					assertNonContradictingAttribute(existing.field().getDeclaringClass(),
							incoming.field().getDeclaringClass(), key, existing, incoming);
				}
			}
		}

		if (anyAttributeField == null && attributes.isEmpty()) {
			throw new IllegalArgumentException("A Gear type must have at least one field annotated with "
					+ TypeName.simple(RetroAnyAttribute.class) + " or " + TypeName.simple(RetroFact.class) + ": "
					+ TypeName.full(type));
		}

		return Optional.of(new GearDescriptor(type, GearType.from(type), matcher, Map.copyOf(attributes),
				factDeclarations, anyAttributeField, idField, sourceField));
	}

	private static void assertIsSourceAri(final Field field, final Class<?> type) {
		if (!ARI.class.equals(field.getType())) {
			throw new IllegalArgumentException(TypeName.simple(RetroSource.class) + " must be used on an "
					+ TypeName.simple(ARI.class) + " field but is used on " + field + ": " + TypeName.full(type));
		}
	}

	private static IllegalArgumentException conflictingAnnotations(final Class<?> first, final Class<?> second,
			final Field field, final Class<?> type) {
		return new IllegalArgumentException(TypeName.simple(first) + " must not be used together with "
				+ TypeName.simple(second) + " on field " + field + ": " + TypeName.full(type));
	}

	private static void assertHasNoArgConstructor(final Class<?> type) {
		try {
			final Constructor<?> ctor = type.getDeclaredConstructor();
			if (!ctor.canAccess(null)) {
				throw new IllegalArgumentException(
						"A Gear type must have a public no-arg constructor: " + TypeName.full(type));
			}
		} catch (final NoSuchMethodException e) {
			throw new IllegalArgumentException("A Gear type must have a no-arg constructor: " + TypeName.full(type), e);
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
		final boolean sameFieldType = a.field().getType().equals(b.field().getType());

		final Class<?> aGeneric = a.singleGenericArgument().orElse(null);
		final Class<?> bGeneric = b.singleGenericArgument().orElse(null);
		final boolean sameGenericType = Objects.equals(aGeneric, bGeneric);

		final FactDescriptor fa = a;
		final FactDescriptor fb = b;

		final boolean sameStrict = fa.isStrict() == fb.isStrict();
		final boolean sameContextual = fa.isContextual() == fb.isContextual();
		final boolean sameParser = fa.parser().equals(fb.parser());

		if (sameOptional && sameFieldType && sameGenericType && sameStrict && sameContextual && sameParser) {
			return;
		}

		final String location = location(typeA, typeB);

		final StringBuilder details = new StringBuilder();
		details.append("optional=").append(fa.isOptional()).append(" vs ").append(fb.isOptional());
		details.append(", strict=").append(fa.isStrict()).append(" vs ").append(fb.isStrict());
		details.append(", contextual=").append(fa.isContextual()).append(" vs ").append(fb.isContextual());
		details.append(", parser=").append(TypeName.full(fa.parser())).append(" vs ")
				.append(TypeName.full(fb.parser()));
		details.append(", fieldType=").append(TypeName.full(fa.field().getType())).append(" vs ")
				.append(TypeName.full(fb.field().getType()));
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
		 * RetroAttribute> at runtime. We at least ensure it is a Map and let
		 * the assignment logic validate key/value types.
		 */
	}
}
