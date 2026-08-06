package com.retrocrawler.core;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IllformedLocaleException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.retrocrawler.core.annotation.RetroClues;
import com.retrocrawler.core.annotation.RetroCollection;
import com.retrocrawler.core.annotation.RetroFactCatalog;
import com.retrocrawler.core.annotation.RetroFactDefaultParser;
import com.retrocrawler.core.archive.clues.ArchiveFolderClueFinder;
import com.retrocrawler.core.archive.filter.ArchivePathFilter;
import com.retrocrawler.core.gear.GearResolver;
import com.retrocrawler.core.gear.GearResolverFactory;
import com.retrocrawler.core.gear.TypeSource;
import com.retrocrawler.core.gear.parser.AutoDetectParser;
import com.retrocrawler.core.gear.parser.CatalogFactParser;
import com.retrocrawler.core.gear.parser.FactCatalogConfiguration;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.core.util.Reflection;
import com.retrocrawler.core.util.TypeName;

/**
 * An immutable, annotation-derived description of a collection and how its
 * archive artifacts are interpreted as gear.
 * <p>
 * A model is deliberately free of archive locations. One crawler applies one
 * model to every archive registered with it.
 */
public final class Model {

	private static final GearResolverFactory GEAR_RESOLVER_FACTORY = new GearResolverFactory();

	private final String collectionId;
	private final String collectionName;
	private final ArchiveFolderClueFinder archiveFolderClueFinder;
	private final Configuration configuration;
	private final GearResolver gearResolver;
	private final Path workingDirectory;
	private final List<ArchivePathFilter> pathFilters;

	private Model(final String collectionId, final String collectionName,
			final ArchiveFolderClueFinder archiveFolderClueFinder, final Configuration configuration,
			final GearResolver gearResolver, final Path workingDirectory, final List<ArchivePathFilter> pathFilters) {
		this.collectionId = Objects.requireNonNull(collectionId, "collectionId");
		this.collectionName = Objects.requireNonNull(collectionName, "collectionName");
		this.archiveFolderClueFinder = Objects.requireNonNull(archiveFolderClueFinder, "archiveFolderClueFinder");
		this.configuration = Objects.requireNonNull(configuration, "configuration");
		this.gearResolver = Objects.requireNonNull(gearResolver, "gearResolver");
		this.workingDirectory = workingDirectory;
		this.pathFilters = List.copyOf(pathFilters);
	}

	/**
	 * Starts annotation-derived model construction with optional runtime
	 * overrides.
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Discovers and reflects on RetroCrawler-annotated types in the given
	 * package and its subpackages.
	 */
	public static Model from(final String basePackage) {
		return builder().typesFrom(basePackage).build();
	}

	/**
	 * Reflects on the caller-supplied set of RetroCrawler model types.
	 */
	public static Model from(final Set<Class<?>> types) {
		return builder().typesFrom(types).build();
	}

	/**
	 * Reflects on the types supplied by an application-specific discovery
	 * source.
	 */
	public static Model from(final TypeSource source) {
		return builder().typesFrom(source).build();
	}

	/** The declared identity of the collection this model interprets. */
	public String collectionId() {
		return collectionId;
	}

	/** The declared display name of the collection, defaulting to its id. */
	public String collectionName() {
		return collectionName;
	}

	public Optional<Path> workingDirectory() {
		return Optional.ofNullable(workingDirectory);
	}

	public ArchiveFolderClueFinder archiveFolderClueFinder() {
		return archiveFolderClueFinder;
	}

	public Configuration configuration() {
		return configuration;
	}

	GearResolver gearResolver() {
		return gearResolver;
	}

	public List<ArchivePathFilter> pathFilters() {
		return pathFilters;
	}

	private static Model create(final Set<Class<?>> types, final Path runtimeWorkingDirectory,
			final List<ArchivePathFilter> runtimePathFilters,
			final Map<Class<? extends CatalogFactParser<?, ?>>, FactCatalogConfiguration> runtimeCatalogConfigurations,
			final Map<Class<? extends FactParser<?>>, Function<String, ? extends FactParser<?>>> runtimeParserFactories,
			final Consumer<Configuration.Builder> runtimeConfiguration) {
		Objects.requireNonNull(types, "types");

		final Set<Class<?>> immutableTypes = types.stream()
				.map(type -> Objects.requireNonNull(type, "types must not contain null"))
				.sorted(Comparator.comparing(Class::getName)).collect(Collectors
						.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), Collections::unmodifiableSet));
		final CollectionDeclaration declaration = collectionDeclaration(immutableTypes);
		final RetroCollection collection = declaration.collection();
		final RetroClues clues = declaration.type().getAnnotation(RetroClues.class);
		if (clues == null) {
			throw new IllegalArgumentException("Missing " + TypeName.simple(RetroClues.class) + " on "
					+ TypeName.simple(RetroCollection.class) + " type " + declaration.type().getName() + ".");
		}

		final Path workingDirectory = runtimeWorkingDirectory == null ? annotationWorkingDirectory(collection)
				: runtimeWorkingDirectory;
		final List<ArchivePathFilter> pathFilters = runtimePathFilters == null
				? Arrays.stream(collection.pathFilters()).<ArchivePathFilter> map(Reflection::newInstance).toList()
				: runtimePathFilters;
		final Map<Class<? extends CatalogFactParser<?, ?>>, FactCatalogConfiguration> catalogConfigurations = effectiveCatalogConfigurations(
				declaration.type(), runtimeCatalogConfigurations);
		final Configuration configuration = effectiveConfiguration(collection, runtimeConfiguration);
		final RetroFactDefaultParser defaultParsers = declaration.type().getAnnotation(RetroFactDefaultParser.class);
		final ArchiveFolderClueFinder clueFinder = ArchiveFolderClueFinder.of(clues);
		final GearResolver gearResolver = GEAR_RESOLVER_FACTORY.reflectOn(immutableTypes, workingDirectory,
				catalogConfigurations, defaultParsers, runtimeParserFactories);

		final String collectionId = collection.id().trim();
		if (collectionId.isEmpty()) {
			throw new IllegalArgumentException(TypeName.simple(RetroCollection.class) + " requires a collection id.");
		}
		final String collectionName = collection.name().isBlank() ? collectionId : collection.name().trim();
		return new Model(collectionId, collectionName, clueFinder, configuration, gearResolver, workingDirectory,
				pathFilters);
	}

	private static Configuration effectiveConfiguration(final RetroCollection collection,
			final Consumer<Configuration.Builder> runtimeConfiguration) {
		final Configuration.Builder annotationDefaults = Configuration.builder();
		final String configuredLocale = collection.locale().trim();
		if (!configuredLocale.isEmpty()) {
			try {
				annotationDefaults.locale(new Locale.Builder().setLanguageTag(configuredLocale).build());
			} catch (final IllformedLocaleException e) {
				throw new IllegalArgumentException(
						"Invalid locale on " + TypeName.simple(RetroCollection.class) + ": " + configuredLocale, e);
			}
		}
		final String configuredTimeZone = collection.timeZone().trim();
		if (!configuredTimeZone.isEmpty()) {
			try {
				annotationDefaults.timeZone(ZoneId.of(configuredTimeZone));
			} catch (final DateTimeException e) {
				throw new IllegalArgumentException(
						"Invalid timeZone on " + TypeName.simple(RetroCollection.class) + ": " + configuredTimeZone, e);
			}
		}

		final Configuration annotationConfiguration = annotationDefaults.build();
		if (runtimeConfiguration == null) {
			return annotationConfiguration;
		}
		final Configuration.Builder runtimeOverrides = annotationConfiguration.toBuilder();
		runtimeConfiguration.accept(runtimeOverrides);
		return runtimeOverrides.build();
	}

	private static CollectionDeclaration collectionDeclaration(final Set<Class<?>> types) {
		RetroCollection collection = null;
		Class<?> annotatedType = null;

		for (final Class<?> type : types) {
			final RetroCollection candidate = type.getAnnotation(RetroCollection.class);
			if (candidate == null) {
				continue;
			}
			if (annotatedType != null) {
				throw new IllegalArgumentException(
						TypeName.simple(RetroCollection.class) + " must be present on exactly one type, but found on "
								+ annotatedType.getName() + " and " + type.getName());
			}
			annotatedType = type;
			collection = candidate;
		}

		if (collection == null) {
			throw new IllegalArgumentException(
					"Missing " + TypeName.simple(RetroCollection.class) + " on provided types.");
		}
		return new CollectionDeclaration(annotatedType, collection);
	}

	private static Path annotationWorkingDirectory(final RetroCollection collection) {
		final String configured = collection.workingDirectory().trim();
		if (configured.isEmpty()) {
			return null;
		}
		try {
			return Path.of(configured);
		} catch (final InvalidPathException e) {
			throw new IllegalArgumentException(
					"Invalid workingDirectory on " + TypeName.simple(RetroCollection.class) + ": " + configured, e);
		}
	}

	private static Map<Class<? extends CatalogFactParser<?, ?>>, FactCatalogConfiguration> effectiveCatalogConfigurations(
			final Class<?> collectionType,
			final Map<Class<? extends CatalogFactParser<?, ?>>, FactCatalogConfiguration> runtimeConfigurations) {
		final Map<Class<? extends CatalogFactParser<?, ?>>, FactCatalogConfiguration> effective = new LinkedHashMap<>();
		for (final RetroFactCatalog annotation : collectionType.getAnnotationsByType(RetroFactCatalog.class)) {
			final String catalogFile = annotation.catalogFile().trim();
			if (catalogFile.isEmpty()) {
				throw new IllegalArgumentException(TypeName.simple(RetroFactCatalog.class) + " for parser "
						+ annotation.parser().getName() + " must override at least one setting.");
			}
			final FactCatalogConfiguration configuration = FactCatalogConfiguration.builder().catalogFile(catalogFile)
					.build();
			if (effective.putIfAbsent(annotation.parser(), configuration) != null) {
				throw new IllegalArgumentException("Duplicate " + TypeName.simple(RetroFactCatalog.class)
						+ " configuration for parser " + annotation.parser().getName() + ".");
			}
		}
		effective.putAll(runtimeConfigurations);
		return Map.copyOf(effective);
	}

	private record CollectionDeclaration(Class<?> type, RetroCollection collection) {
	}

	/**
	 * Builds a model from portable annotation defaults and optional runtime
	 * deployment overrides.
	 */
	public static final class Builder {

		private Set<Class<?>> types;
		private Path workingDirectory;
		private List<ArchivePathFilter> pathFilters;
		private final Map<Class<? extends CatalogFactParser<?, ?>>, FactCatalogConfiguration> catalogConfigurations = new LinkedHashMap<>();
		private final Map<Class<? extends FactParser<?>>, Function<String, ? extends FactParser<?>>> parserFactories = new LinkedHashMap<>();
		private Consumer<Configuration.Builder> configuration;

		private Builder() {
		}

		/**
		 * Discovers model types below the given package.
		 */
		public Builder typesFrom(final String basePackage) {
			types = ModelTypeDiscovery.discover(basePackage);
			return this;
		}

		/**
		 * Uses an explicit set of annotation-bearing model types.
		 */
		public Builder typesFrom(final Set<Class<?>> modelTypes) {
			types = Set.copyOf(Objects.requireNonNull(modelTypes, "modelTypes"));
			return this;
		}

		/**
		 * Obtains explicit model types from an application extension point.
		 */
		public Builder typesFrom(final TypeSource source) {
			Objects.requireNonNull(source, "source");
			return typesFrom(Objects.requireNonNull(source.types(), "source.types()"));
		}

		/**
		 * Overrides the collection working directory declared by
		 * {@link RetroCollection}.
		 */
		public Builder workingDirectory(final Path path) {
			workingDirectory = Objects.requireNonNull(path, "path");
			return this;
		}

		/**
		 * Overrides the archive path filters declared by
		 * {@link RetroCollection}.
		 */
		public Builder pathFilters(final ArchivePathFilter... filters) {
			Objects.requireNonNull(filters, "filters");
			return pathFilters(List.of(filters));
		}

		/**
		 * Overrides the archive path filters declared by
		 * {@link RetroCollection}.
		 */
		public Builder pathFilters(final Collection<? extends ArchivePathFilter> filters) {
			Objects.requireNonNull(filters, "filters");
			pathFilters = filters.stream()
					.map(filter -> Objects.requireNonNull(filter, "filters must not contain null"))
					.map(ArchivePathFilter.class::cast).toList();
			return this;
		}

		/**
		 * Overrides collection-wide interpretation configuration after
		 * annotation defaults have been applied.
		 */
		public Builder configuration(final Consumer<Configuration.Builder> customizer) {
			if (configuration != null) {
				throw new IllegalStateException("Configuration is already configured.");
			}
			configuration = Objects.requireNonNull(customizer, "customizer");
			return this;
		}

		/**
		 * Overrides the catalog used by a catalog-backed parser if that parser
		 * is selected by a discovered fact declaration.
		 */
		public Builder factCatalog(final Class<? extends CatalogFactParser<?, ?>> parser,
				final Consumer<FactCatalogConfiguration.Builder> customizer) {
			Objects.requireNonNull(parser, "parser");
			Objects.requireNonNull(customizer, "customizer");
			final FactCatalogConfiguration.Builder configuration = FactCatalogConfiguration.builder();
			customizer.accept(configuration);
			if (catalogConfigurations.putIfAbsent(parser, configuration.build()) != null) {
				throw new IllegalArgumentException("Fact catalog is configured more than once: " + parser.getName());
			}
			return this;
		}

		/**
		 * Supplies per-fact-key construction for a parser class selected either
		 * explicitly or as a default. The factory receives the effective fact
		 * key and is invoked once for each matching key. It may return any
		 * parser implementation for the same fact value type. Without a
		 * registered factory, the framework uses its standard construction for
		 * the selected class.
		 */
		public <T> Builder parserFactory(final Class<? extends FactParser<T>> parserType,
				final Function<String, ? extends FactParser<T>> factory) {
			Objects.requireNonNull(parserType, "parserType");
			Objects.requireNonNull(factory, "factory");
			if (parserType.equals(AutoDetectParser.class)) {
				throw new IllegalArgumentException(TypeName.simple(AutoDetectParser.class)
						+ " selects another parser class and is not itself constructed.");
			}
			final Class<? extends FactParser<?>> untypedParserType = parserType;
			final Function<String, ? extends FactParser<?>> untypedFactory = factory;
			if (parserFactories.putIfAbsent(untypedParserType, untypedFactory) != null) {
				throw new IllegalArgumentException(
						"Parser factory is configured more than once: " + parserType.getName());
			}
			return this;
		}

		/**
		 * Validates the combined annotation and runtime configuration and
		 * creates an immutable model.
		 */
		public Model build() {
			if (types == null) {
				throw new IllegalStateException("Model types must be configured before building a model.");
			}
			return create(types, workingDirectory, pathFilters, Map.copyOf(catalogConfigurations),
					Map.copyOf(parserFactories), configuration);
		}
	}
}
