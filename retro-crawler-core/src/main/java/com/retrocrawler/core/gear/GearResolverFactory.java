package com.retrocrawler.core.gear;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.core.annotation.RetroId;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.gear.parser.AutoDetectParser;
import com.retrocrawler.core.gear.parser.EnumParser;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.core.gear.parser.IntParser;
import com.retrocrawler.core.gear.parser.StringParser;
import com.retrocrawler.core.util.Reflection;
import com.retrocrawler.core.util.TypeName;

public class GearResolverFactory implements ReflectiveFactory<GearResolver> {

	private record GlobalIdDefinition(boolean standalone, String key) {
	}

	@Override
	public GearResolver reflectOn(final Set<Class<?>> types) {
		Objects.requireNonNull(types, "types");

		/*
		 * Look for retro gear. Not all types are required to be annotated with
		 * RetroGear. Some might end up in the Set because of package scanning; One
		 * might only serve as the archive descriptor.
		 */
		final Map<Class<?>, GearSpecialist> specialists = new HashMap<>();
		for (final Class<?> type : types) {
			GearDescriptor.of(type).ifPresent(gd -> {
				specialists.put(type, new GearSpecialist(gd));
			});
		}
		if (specialists.isEmpty()) {
			throw new IllegalArgumentException(
					"At least one type must be annotated with " + TypeName.simple(RetroGear.class));
		}

		assertConsistentRetroId(specialists);

		// Collect all known attribute definitions and ensure no contradictions.
		final Map<String, FactDescriptor> attributes = new HashMap<>();
		final Map<String, Class<?>> declaringTypes = new HashMap<>();

		for (final GearSpecialist specialist : specialists.values()) {
			final GearDescriptor definition = specialist.getGearDefinition();
			final Class<?> type = definition.getType();

			for (final Entry<String, FactDescriptor> entry : definition.getAttributes().entrySet()) {
				final String key = entry.getKey();
				final FactDescriptor incoming = entry.getValue();

				final FactDescriptor existing = attributes.putIfAbsent(key, incoming);
				if (existing == null) {
					declaringTypes.put(key, type);
				} else {
					final Class<?> firstType = declaringTypes.get(key);
					GearDescriptor.assertNonContradictingAttribute(firstType, type, key, existing, incoming);
				}
			}
		}

		// Build FactFinders (one per key) for FactDefinition only.
		final Map<String, FactFinder> factFinders = new HashMap<>();

		for (final Entry<String, FactDescriptor> entry : attributes.entrySet()) {
			final String key = entry.getKey();
			final FactDescriptor attrDef = entry.getValue();

			if (!(attrDef instanceof FactDescriptor)) {
				throw new IllegalStateException("Unexpected " + TypeName.simple(FactDescriptor.class)
						+ " type for key '" + key + "': " + TypeName.full(attrDef.getClass()));
			}

			final FactDescriptor factDef = attrDef;

			final Class<? extends FactParser> parserType = factDef.getParser();
			final FactParser parser;

			if (parserType.equals(AutoDetectParser.class)) {
				parser = autoDetectParser(key, factDef);
			} else {
				parser = Reflection.newInstance(parserType);
			}

			final Class<?> fieldType = factDef.getField().getType();
			final boolean strict = factDef.isStrict();

			factFinders.put(key, new FactFinder(key, parser, fieldType, strict));
		}

		return new GearResolver(specialists, Map.copyOf(factFinders));
	}

	// TODO turn into configurable factory so users can supply their own default
	// parsers
	private static FactParser autoDetectParser(final String key, final FactDescriptor attrDef) {
		Objects.requireNonNull(key, "key");
		Objects.requireNonNull(attrDef, "attrDef");

		final Class<?> fieldType = attrDef.getField().getType();
		final var genericType = attrDef.getSingleGenericArgument();

		if (fieldType == String.class) {
			return new StringParser();
		}

		if (fieldType == int.class || fieldType == Integer.class) {
			return new IntParser();
		}

		if (fieldType.isEnum()) {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			final Class<? extends Enum> enumType = (Class<? extends Enum>) fieldType;
			return new EnumParser<>(enumType, true);
		}

		if (java.util.EnumSet.class.isAssignableFrom(fieldType)) {
			if (genericType.isEmpty()) {
				throw new UnsupportedOperationException("Auto-detected field type '" + TypeName.full(fieldType)
						+ "' for key '" + key
						+ "', but EnumSet element type is missing. Use a parameterized EnumSet<...> "
						+ "or specify a parser explicitly (not " + TypeName.simple(AutoDetectParser.class) + ").");
			}
			final Class<?> elementType = genericType.get();
			throw new UnsupportedOperationException("Auto-detected EnumSet<" + TypeName.full(elementType)
					+ "> for key '" + key + "', but no parser is implemented yet.");
		}

		if (java.util.Collection.class.isAssignableFrom(fieldType)) {
			if (genericType.isEmpty()) {
				throw new UnsupportedOperationException("Auto-detected field type '" + TypeName.full(fieldType)
						+ "' for key '" + key
						+ "', but collection element type is missing. Use a parameterized collection type "
						+ "or specify a parser explicitly (not " + TypeName.simple(AutoDetectParser.class) + ").");
			}
			final Class<?> elementType = genericType.get();
			throw new UnsupportedOperationException("Auto-detected " + TypeName.full(fieldType) + "<"
					+ TypeName.full(elementType) + "> for key '" + key + "', but no parser is implemented yet.");
		}

		throw new IllegalArgumentException("Cannot auto-detect parser for key '" + key + "' and field type '"
				+ TypeName.full(fieldType) + "'. Please specify a parser explicitly (not "
				+ TypeName.simple(AutoDetectParser.class) + ").");
	}

	private static void assertConsistentRetroId(final Map<Class<?>, GearSpecialist> specialists) {
		Objects.requireNonNull(specialists, "specialists");

		GlobalIdDefinition global = null;
		Class<?> firstType = null;

		for (final GearSpecialist specialist : specialists.values()) {
			final GearDescriptor def = specialist.getGearDefinition();
			final Class<?> gearType = def.getType();

			final Optional<Field> idFieldOpt = def.getIdField();
			if (idFieldOpt.isEmpty()) {
				continue;
			}

			final Field idField = idFieldOpt.get();

			boolean standalone = true;
			String key = null;

			for (final Entry<String, FactDescriptor> e : def.getAttributes().entrySet()) {
				final FactDescriptor attrDef = e.getValue();
				if (attrDef.getField().equals(idField)) {
					standalone = false;
					key = e.getKey();
					break;
				}
			}

			if (standalone) {
				key = Clue.KEY_INTERNAL_ID;
			}

			final GlobalIdDefinition current = new GlobalIdDefinition(standalone, key);

			if (global == null) {
				global = current;
				firstType = gearType;
				continue;
			}

			if (!global.equals(current)) {
				throw new IllegalArgumentException("Inconsistent " + TypeName.simple(RetroId.class)
						+ " configuration between " + TypeName.full(firstType) + " and " + TypeName.full(gearType)
						+ ". Expected " + describeGlobalId(global) + " but got " + describeGlobalId(current) + ".");
			}
		}
	}

	private static String describeGlobalId(final GlobalIdDefinition def) {
		if (def.standalone()) {
			return "standalone id from key '" + def.key() + "'";
		}
		return "id from key '" + def.key() + "'";
	}

}