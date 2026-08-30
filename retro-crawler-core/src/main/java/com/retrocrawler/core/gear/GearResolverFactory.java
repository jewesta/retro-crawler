package com.retrocrawler.core.gear;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.retrocrawler.core.annotation.RetroFactDefaultParser;
import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.core.annotation.RetroId;
import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.archive.clues.InternalClueKeys;
import com.retrocrawler.core.catalog.CatalogLoader;
import com.retrocrawler.core.gear.filter.FilterDefinition;
import com.retrocrawler.core.gear.injector.GearSpecialist;
import com.retrocrawler.core.gear.matcher.AnyGearMatcher;
import com.retrocrawler.core.gear.parser.ARIParser;
import com.retrocrawler.core.gear.parser.AutoDetectParser;
import com.retrocrawler.core.gear.parser.CatalogFactParser;
import com.retrocrawler.core.gear.parser.EnumFactParser;
import com.retrocrawler.core.gear.parser.EnumParser;
import com.retrocrawler.core.gear.parser.FactCatalogConfiguration;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.core.gear.parser.InstantParser;
import com.retrocrawler.core.gear.parser.IntParser;
import com.retrocrawler.core.gear.parser.LocalDateParser;
import com.retrocrawler.core.gear.parser.StringParser;
import com.retrocrawler.core.util.Reflection;
import com.retrocrawler.core.util.TypeName;

public class GearResolverFactory implements ReflectiveFactory<GearResolver> {

	private record GlobalIdDefinition(boolean standalone, String key) {
	}

	@Override
	public GearResolver reflectOn(final Set<Class<?>> types) {
		return reflectOn(types, null, Map.of(), null, Map.of());
	}

	public GearResolver reflectOn(final Set<Class<?>> types, final Path workingDirectory,
			final Map<Class<? extends CatalogFactParser<?, ?>>, FactCatalogConfiguration> catalogConfigurations) {
		return reflectOn(types, workingDirectory, catalogConfigurations, null, Map.of());
	}

	public GearResolver reflectOn(final Set<Class<?>> types, final Path workingDirectory,
			final Map<Class<? extends CatalogFactParser<?, ?>>, FactCatalogConfiguration> catalogConfigurations,
			final RetroFactDefaultParser defaultParsers,
			final Map<Class<? extends FactParser<?>>, Function<String, ? extends FactParser<?>>> parserFactories) {
		Objects.requireNonNull(types, "types");
		Objects.requireNonNull(catalogConfigurations, "catalogConfigurations");
		Objects.requireNonNull(parserFactories, "parserFactories");

		/*
		 * Look for retro gear. Not all types are required to be annotated with
		 * RetroGear. Some might end up in the Set because of package scanning;
		 * One might only serve as the archive descriptor.
		 */
		final Map<Class<?>, GearSpecialist> specialists = new LinkedHashMap<>();
		for (final Class<?> type : types) {
			GearDescriptor.of(type).ifPresent(gd -> specialists.put(type, new GearSpecialist(gd)));
		}
		if (specialists.isEmpty()) {
			throw new IllegalArgumentException(
					"At least one type must be annotated with " + TypeName.simple(RetroGear.class));
		}

		assertUniqueAnyGearMatcher(specialists);
		assertConsistentRetroId(specialists);

		// Collect all known attribute definitions and ensure no contradictions.
		final Map<String, FactDescriptor> attributes = new LinkedHashMap<>();
		final Map<String, Class<?>> declaringTypes = new HashMap<>();
		final Map<Class<?>, Set<String>> contextualFactKeys = new HashMap<>();
		final Map<String, Map<Class<?>, Field>> filterBindings = new LinkedHashMap<>();

		for (final GearSpecialist specialist : specialists.values()) {
			final GearDescriptor definition = specialist.gearDefinition();
			final Class<?> type = definition.type();
			final Set<String> contextualKeys = new HashSet<>();

			for (final Entry<String, FactDescriptor> entry : definition.attributes().entrySet()) {
				final String key = entry.getKey();
				final FactDescriptor incoming = entry.getValue();
				filterBindings.computeIfAbsent(key, ignored -> new LinkedHashMap<>()).put(type, incoming.field());
				if (incoming.isContextual()) {
					contextualKeys.add(key);
				}

				final FactDescriptor existing = attributes.putIfAbsent(key, incoming);
				if (existing == null) {
					declaringTypes.put(key, type);
				} else {
					final Class<?> firstType = declaringTypes.get(key);
					GearDescriptor.assertNonContradictingAttribute(firstType, type, key, existing, incoming);
				}
			}
			contextualFactKeys.put(type, Set.copyOf(contextualKeys));
		}

		// Build FactFinders (one per key) for FactDefinition only.
		final Map<String, FactFinder> factFinders = new LinkedHashMap<>();
		final List<FilterDefinition<?>> filters = new ArrayList<>();

		final List<Entry<String, FactDescriptor>> orderedAttributes = attributes.entrySet().stream()
				.sorted(Entry.comparingByKey()).toList();
		for (final Entry<String, FactDescriptor> entry : orderedAttributes) {
			final String key = entry.getKey();
			final FactDescriptor attrDef = entry.getValue();

			if (!(attrDef instanceof FactDescriptor)) {
				throw new IllegalStateException("Unexpected " + TypeName.simple(FactDescriptor.class)
						+ " type for key '" + key + "': " + TypeName.full(attrDef.getClass()));
			}

			final FactDescriptor factDef = attrDef;

			final Class<? extends FactParser<?>> parserType = factDef.parser();
			final FactParser<?> parser;

			if (parserType.equals(AutoDetectParser.class)) {
				parser = autoDetectParser(key, factDef, workingDirectory, catalogConfigurations, defaultParsers,
						parserFactories);
			} else {
				parser = configuredParser(key, parserType, workingDirectory, catalogConfigurations.get(parserType),
						parserFactories);
			}

			final Class<?> fieldType = factDef.field().getType();
			final boolean strict = factDef.isStrict();

			factFinders.put(key, new FactFinder(key, parser, fieldType, strict, factDef.isContextual()));
			filters.add(ReflectedFilterDefinition.create(key, factDef,
					Objects.requireNonNull(parser.filterType(), "filterType for " + parser.getClass().getName()),
					filterBindings.get(key)));
		}

		return new GearResolver(Map.copyOf(specialists), Map.copyOf(factFinders), Map.copyOf(contextualFactKeys),
				List.copyOf(filters));
	}

	private static void assertUniqueAnyGearMatcher(final Map<Class<?>, GearSpecialist> specialists) {
		Class<?> fallbackType = null;
		for (final GearSpecialist specialist : specialists.values()) {
			final GearDescriptor descriptor = specialist.gearDefinition();
			if (!(descriptor.matcher() instanceof AnyGearMatcher)) {
				continue;
			}
			if (fallbackType != null) {
				throw new IllegalArgumentException(TypeName.simple(AnyGearMatcher.class)
						+ " may be assigned to only one Gear type, but is assigned to " + TypeName.full(fallbackType)
						+ " and " + TypeName.full(descriptor.type()) + ".");
			}
			fallbackType = descriptor.type();
		}
	}

	private static FactParser<?> configuredParser(final String key, final Class<? extends FactParser<?>> parserType,
			final Path workingDirectory, final FactCatalogConfiguration configuration,
			final Map<Class<? extends FactParser<?>>, Function<String, ? extends FactParser<?>>> parserFactories) {
		final Function<String, ? extends FactParser<?>> factory = parserFactories.get(parserType);
		if (factory != null) {
			return Objects.requireNonNull(factory.apply(key),
					"Parser factory for " + parserType.getName() + " returned null for key '" + key + "'.");
		}

		if (!CatalogFactParser.class.isAssignableFrom(parserType)) {
			if (configuration != null && configuration.catalogFile().isPresent()) {
				throw new IllegalArgumentException("Fact parser " + parserType.getName() + " does not implement "
						+ CatalogFactParser.class.getSimpleName() + " and cannot use catalogFile configuration.");
			}
			return Reflection.newInstance(parserType);
		}

		final CatalogLoader catalogs = new DefaultCatalogLoader(parserType, workingDirectory, configuration);
		try {
			final Constructor<? extends FactParser<?>> constructor = parserType.getConstructor(CatalogLoader.class);
			return constructor.newInstance(catalogs);
		} catch (final NoSuchMethodException e) {
			throw new IllegalArgumentException("Catalog fact parser " + parserType.getName()
					+ " must have a public constructor accepting " + CatalogLoader.class.getSimpleName() + ".", e);
		} catch (final InstantiationException | IllegalAccessException e) {
			throw new IllegalArgumentException("Cannot instantiate catalog fact parser: " + parserType.getName(), e);
		} catch (final InvocationTargetException e) {
			final Throwable cause = e.getCause();
			if (cause instanceof final RuntimeException runtime) {
				throw runtime;
			}
			throw new IllegalArgumentException("Cannot instantiate catalog fact parser: " + parserType.getName(),
					cause);
		}
	}

	private static FactParser<?> configuredEnumParser(final String key, final Class<?> enumType,
			final Class<? extends EnumFactParser> parserType,
			final Map<Class<? extends FactParser<?>>, Function<String, ? extends FactParser<?>>> parserFactories) {
		final Function<String, ? extends FactParser<?>> factory = parserFactories.get(parserType);
		if (factory != null) {
			return Objects.requireNonNull(factory.apply(key),
					"Parser factory for " + parserType.getName() + " returned null for key '" + key + "'.");
		}

		try {
			final Constructor<? extends EnumFactParser> constructor = parserType.getConstructor(Class.class);
			return constructor.newInstance(enumType);
		} catch (final NoSuchMethodException e) {
			throw new IllegalArgumentException("Default enum fact parser " + parserType.getName()
					+ " must have a public constructor accepting Class.", e);
		} catch (final InstantiationException | IllegalAccessException e) {
			throw new IllegalArgumentException("Cannot instantiate default enum fact parser: " + parserType.getName(),
					e);
		} catch (final InvocationTargetException e) {
			final Throwable cause = e.getCause();
			if (cause instanceof final RuntimeException runtime) {
				throw runtime;
			}
			throw new IllegalArgumentException("Cannot instantiate default enum fact parser: " + parserType.getName(),
					cause);
		}
	}

	private static FactParser<?> autoDetectParser(final String key, final FactDescriptor attrDef,
			final Path workingDirectory,
			final Map<Class<? extends CatalogFactParser<?, ?>>, FactCatalogConfiguration> catalogConfigurations,
			final RetroFactDefaultParser defaultParsers,
			final Map<Class<? extends FactParser<?>>, Function<String, ? extends FactParser<?>>> parserFactories) {
		Objects.requireNonNull(key, "key");
		Objects.requireNonNull(attrDef, "attrDef");

		final Class<?> fieldType = attrDef.field().getType();
		final var genericType = attrDef.singleGenericArgument();

		if (fieldType == String.class) {
			final Class<? extends FactParser<String>> parserType = defaultParsers == null ? StringParser.class
					: defaultParsers.string();
			return configuredParser(key, parserType, workingDirectory, catalogConfigurations.get(parserType),
					parserFactories);
		}

		if (fieldType == int.class || fieldType == Integer.class) {
			final Class<? extends FactParser<Integer>> parserType = defaultParsers == null ? IntParser.class
					: defaultParsers.integer();
			return configuredParser(key, parserType, workingDirectory, catalogConfigurations.get(parserType),
					parserFactories);
		}

		if (fieldType == Instant.class) {
			final Class<? extends FactParser<Instant>> parserType = defaultParsers == null ? InstantParser.class
					: defaultParsers.instant();
			return configuredParser(key, parserType, workingDirectory, catalogConfigurations.get(parserType),
					parserFactories);
		}

		if (fieldType == LocalDate.class) {
			final Class<? extends FactParser<LocalDate>> parserType = defaultParsers == null ? LocalDateParser.class
					: defaultParsers.localDate();
			return configuredParser(key, parserType, workingDirectory, catalogConfigurations.get(parserType),
					parserFactories);
		}

		if (fieldType == ARI.class) {
			final Class<? extends FactParser<ARI>> parserType = defaultParsers == null ? ARIParser.class
					: defaultParsers.ari();
			return configuredParser(key, parserType, workingDirectory, catalogConfigurations.get(parserType),
					parserFactories);
		}

		if (fieldType.isEnum()) {
			@SuppressWarnings("rawtypes")
			final Class<? extends EnumFactParser> parserType = defaultParsers == null ? EnumParser.class
					: defaultParsers.enumeration();
			return configuredEnumParser(key, fieldType, parserType, parserFactories);
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
			if (elementType == ARI.class) {
				final Class<? extends FactParser<ARI>> parserType = defaultParsers == null ? ARIParser.class
						: defaultParsers.ari();
				return configuredParser(key, parserType, workingDirectory, catalogConfigurations.get(parserType),
						parserFactories);
			}
			if (elementType == Instant.class) {
				final Class<? extends FactParser<Instant>> parserType = defaultParsers == null ? InstantParser.class
						: defaultParsers.instant();
				return configuredParser(key, parserType, workingDirectory, catalogConfigurations.get(parserType),
						parserFactories);
			}
			if (elementType == LocalDate.class) {
				final Class<? extends FactParser<LocalDate>> parserType = defaultParsers == null ? LocalDateParser.class
						: defaultParsers.localDate();
				return configuredParser(key, parserType, workingDirectory, catalogConfigurations.get(parserType),
						parserFactories);
			}
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
			final GearDescriptor def = specialist.gearDefinition();
			final Class<?> gearType = def.type();

			final Optional<Field> idFieldOpt = def.idField();
			if (idFieldOpt.isEmpty()) {
				continue;
			}

			final Field idField = idFieldOpt.get();

			boolean standalone = true;
			String key = null;

			for (final Entry<String, FactDescriptor> e : def.attributes().entrySet()) {
				final FactDescriptor attrDef = e.getValue();
				if (attrDef.field().equals(idField)) {
					standalone = false;
					key = e.getKey();
					break;
				}
			}

			if (standalone) {
				key = InternalClueKeys.ID;
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
