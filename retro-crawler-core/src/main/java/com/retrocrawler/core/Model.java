package com.retrocrawler.core;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.retrocrawler.core.annotation.RetroClues;
import com.retrocrawler.core.annotation.RetroCollection;
import com.retrocrawler.core.annotation.RetroFactParser;
import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveRoots;
import com.retrocrawler.core.archive.CrawlPolicy;
import com.retrocrawler.core.archive.clues.ArchivePathClueFinder;
import com.retrocrawler.core.gear.GearResolver;
import com.retrocrawler.core.gear.GearResolverFactory;
import com.retrocrawler.core.gear.TypeSource;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.core.gear.parser.FactParserConfiguration;
import com.retrocrawler.core.util.Reflection;
import com.retrocrawler.core.util.TypeName;

/**
 * An immutable, annotation-derived description of a collection and how its
 * archive artifacts are interpreted as gear.
 */
public final class Model {

	private static final GearResolverFactory GEAR_RESOLVER_FACTORY = new GearResolverFactory();

	private final ArchiveDescriptor archiveDescriptor;
	private final ArchivePathClueFinder archivePathClueFinder;
	private final GearResolver gearResolver;
	private final Path workingDirectory;
	private final CrawlPolicy crawlPolicy;

	private Model(final ArchiveDescriptor archiveDescriptor, final ArchivePathClueFinder archivePathClueFinder,
			final GearResolver gearResolver, final Path workingDirectory, final CrawlPolicy crawlPolicy) {
		this.archiveDescriptor = Objects.requireNonNull(archiveDescriptor, "archiveDescriptor");
		this.archivePathClueFinder = Objects.requireNonNull(archivePathClueFinder, "archivePathClueFinder");
		this.gearResolver = Objects.requireNonNull(gearResolver, "gearResolver");
		this.workingDirectory = workingDirectory;
		this.crawlPolicy = Objects.requireNonNull(crawlPolicy, "crawlPolicy");
	}

	/**
	 * Starts annotation-derived model construction with optional runtime overrides.
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Discovers and reflects on RetroCrawler-annotated types in the given package
	 * and its subpackages.
	 */
	public static Model from(final String basePackage) {
		return builder().typesFrom(basePackage).build();
	}

	/**
	 * Discovers model types in the given package while supplying collection
	 * locations as runtime deployment configuration.
	 */
	public static Model from(final String basePackage, final ArchiveRoots archiveRoots) {
		return builder().typesFrom(basePackage).locations(archiveRoots).build();
	}

	/**
	 * Reflects on the caller-supplied set of RetroCrawler model types.
	 */
	public static Model from(final Set<Class<?>> types) {
		return builder().typesFrom(types).build();
	}

	/**
	 * Reflects on caller-supplied model types while supplying collection locations
	 * as runtime deployment configuration.
	 */
	public static Model from(final Set<Class<?>> types, final ArchiveRoots archiveRoots) {
		return builder().typesFrom(types).locations(archiveRoots).build();
	}

	/**
	 * Reflects on the types supplied by an application-specific discovery source.
	 */
	public static Model from(final TypeSource source) {
		return builder().typesFrom(source).build();
	}

	/**
	 * Reflects on application-supplied model types while supplying collection
	 * locations as runtime deployment configuration.
	 */
	public static Model from(final TypeSource source, final ArchiveRoots archiveRoots) {
		return builder().typesFrom(source).locations(archiveRoots).build();
	}

	public ArchiveDescriptor archiveDescriptor() {
		return archiveDescriptor;
	}

	public Optional<Path> workingDirectory() {
		return Optional.ofNullable(workingDirectory);
	}

	ArchivePathClueFinder archivePathClueFinder() {
		return archivePathClueFinder;
	}

	GearResolver gearResolver() {
		return gearResolver;
	}

	CrawlPolicy crawlPolicy() {
		return crawlPolicy;
	}

	private static Model create(final Set<Class<?>> types, final ArchiveRoots archiveRoots,
			final Path runtimeWorkingDirectory, final CrawlPolicy runtimeCrawlPolicy,
			final Map<Class<? extends FactParser>, FactParserConfiguration> runtimeParserConfigurations) {
		Objects.requireNonNull(types, "types");

		final Set<Class<?>> immutableTypes = types.stream()
				.map(type -> Objects.requireNonNull(type, "types must not contain null"))
				.sorted(Comparator.comparing(Class::getName))
				.collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new),
						Collections::unmodifiableSet));
		final CollectionDeclaration declaration = collectionDeclaration(immutableTypes);
		final RetroCollection collection = declaration.collection();
		final RetroClues clues = declaration.type().getAnnotation(RetroClues.class);
		if (clues == null) {
			throw new IllegalArgumentException("Missing " + TypeName.simple(RetroClues.class) + " on "
					+ TypeName.simple(RetroCollection.class) + " type " + declaration.type().getName() + ".");
		}

		final ArchiveDescriptor descriptor = archiveRoots == null ? ArchiveDescriptor.of(collection)
				: ArchiveDescriptor.of(collection, archiveRoots);
		final Path workingDirectory = runtimeWorkingDirectory == null ? annotationWorkingDirectory(collection)
				: runtimeWorkingDirectory;
		final CrawlPolicy crawlPolicy = runtimeCrawlPolicy == null
				? Reflection.newInstance(collection.crawlPolicy())
				: runtimeCrawlPolicy;
		final Map<Class<? extends FactParser>, FactParserConfiguration> parserConfigurations =
				effectiveParserConfigurations(declaration.type(), runtimeParserConfigurations);
		final ArchivePathClueFinder clueFinder = ArchivePathClueFinder.of(clues);
		final GearResolver gearResolver = GEAR_RESOLVER_FACTORY.reflectOn(
				immutableTypes, workingDirectory, parserConfigurations);

		return new Model(descriptor, clueFinder, gearResolver, workingDirectory, crawlPolicy);
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
			throw new IllegalArgumentException("Invalid workingDirectory on "
					+ TypeName.simple(RetroCollection.class) + ": " + configured, e);
		}
	}

	private static Map<Class<? extends FactParser>, FactParserConfiguration> effectiveParserConfigurations(
			final Class<?> collectionType,
			final Map<Class<? extends FactParser>, FactParserConfiguration> runtimeConfigurations) {
		final Map<Class<? extends FactParser>, FactParserConfiguration> effective = new LinkedHashMap<>();
		for (final RetroFactParser annotation : collectionType.getAnnotationsByType(RetroFactParser.class)) {
			final String catalogFile = annotation.catalogFile().trim();
			if (catalogFile.isEmpty()) {
				throw new IllegalArgumentException(TypeName.simple(RetroFactParser.class) + " for parser "
						+ annotation.parser().getName() + " must override at least one setting.");
			}
			final FactParserConfiguration configuration = FactParserConfiguration.builder()
					.catalogFile(catalogFile).build();
			if (effective.putIfAbsent(annotation.parser(), configuration) != null) {
				throw new IllegalArgumentException("Duplicate " + TypeName.simple(RetroFactParser.class)
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
		private ArchiveRoots archiveRoots;
		private Path workingDirectory;
		private CrawlPolicy crawlPolicy;
		private final Map<Class<? extends FactParser>, FactParserConfiguration> parserConfigurations =
				new LinkedHashMap<>();

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
		 * Overrides the collection locations declared by {@link RetroCollection}.
		 */
		public Builder locations(final Path... rootPaths) {
			return locations(ArchiveRoots.from(rootPaths));
		}

		/**
		 * Overrides the collection locations declared by {@link RetroCollection}.
		 */
		public Builder locations(final Collection<Path> rootPaths) {
			return locations(ArchiveRoots.from(rootPaths));
		}

		/**
		 * Overrides the collection locations declared by {@link RetroCollection}.
		 */
		public Builder locations(final ArchiveRoots roots) {
			archiveRoots = Objects.requireNonNull(roots, "roots");
			return this;
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
		 * Overrides the crawl policy declared by {@link RetroCollection}.
		 */
		public Builder crawlPolicy(final CrawlPolicy policy) {
			crawlPolicy = Objects.requireNonNull(policy, "policy");
			return this;
		}

		/**
		 * Overrides standard configuration for a parser if that parser is selected by
		 * a discovered fact declaration.
		 */
		public Builder factParser(final Class<? extends FactParser> parser,
				final Consumer<FactParserConfiguration.Builder> customizer) {
			Objects.requireNonNull(parser, "parser");
			Objects.requireNonNull(customizer, "customizer");
			final FactParserConfiguration.Builder configuration = FactParserConfiguration.builder();
			customizer.accept(configuration);
			if (parserConfigurations.putIfAbsent(parser, configuration.build()) != null) {
				throw new IllegalArgumentException("Fact parser is configured more than once: " + parser.getName());
			}
			return this;
		}

		/**
		 * Validates the combined annotation and runtime configuration and creates an
		 * immutable model.
		 */
		public Model build() {
			if (types == null) {
				throw new IllegalStateException("Model types must be configured before building a model.");
			}
			return create(types, archiveRoots, workingDirectory, crawlPolicy, Map.copyOf(parserConfigurations));
		}
	}
}
