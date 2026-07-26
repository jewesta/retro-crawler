package com.retrocrawler.core;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.retrocrawler.core.annotation.RetroArchive;
import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.clues.ArchivePathClueFinder;
import com.retrocrawler.core.gear.GearResolver;
import com.retrocrawler.core.gear.GearResolverFactory;
import com.retrocrawler.core.gear.TypeSource;
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

	private Model(final ArchiveDescriptor archiveDescriptor, final ArchivePathClueFinder archivePathClueFinder,
			final GearResolver gearResolver) {
		this.archiveDescriptor = Objects.requireNonNull(archiveDescriptor, "archiveDescriptor");
		this.archivePathClueFinder = Objects.requireNonNull(archivePathClueFinder, "archivePathClueFinder");
		this.gearResolver = Objects.requireNonNull(gearResolver, "gearResolver");
	}

	/**
	 * Discovers and reflects on RetroCrawler-annotated types in the given package
	 * and its subpackages.
	 */
	public static Model from(final String basePackage) {
		return from(ModelTypeDiscovery.discover(basePackage));
	}

	/**
	 * Reflects on the caller-supplied set of RetroCrawler model types.
	 */
	public static Model from(final Set<Class<?>> types) {
		Objects.requireNonNull(types, "types");

		final Set<Class<?>> immutableTypes = types.stream()
				.map(type -> Objects.requireNonNull(type, "types must not contain null"))
				.sorted(Comparator.comparing(Class::getName))
				.collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new),
						Collections::unmodifiableSet));
		final RetroArchive archive = assertRetroArchiveOnOneOf(immutableTypes);
		final ArchiveDescriptor descriptor = ArchiveDescriptor.of(archive);
		final ArchivePathClueFinder clueFinder = ArchivePathClueFinder.of(archive.findClues());
		final GearResolver gearResolver = GEAR_RESOLVER_FACTORY.reflectOn(immutableTypes);

		return new Model(descriptor, clueFinder, gearResolver);
	}

	/**
	 * Reflects on the types supplied by an application-specific discovery source.
	 */
	public static Model from(final TypeSource source) {
		Objects.requireNonNull(source, "source");
		return from(Objects.requireNonNull(source.getTypes(), "source.getTypes()"));
	}

	public ArchiveDescriptor getArchiveDescriptor() {
		return archiveDescriptor;
	}

	ArchivePathClueFinder archivePathClueFinder() {
		return archivePathClueFinder;
	}

	GearResolver gearResolver() {
		return gearResolver;
	}

	private static RetroArchive assertRetroArchiveOnOneOf(final Set<Class<?>> types) {
		RetroArchive archive = null;
		Class<?> annotatedType = null;

		for (final Class<?> type : types) {
			final RetroArchive candidate = type.getAnnotation(RetroArchive.class);
			if (candidate == null) {
				continue;
			}
			if (annotatedType != null) {
				throw new IllegalArgumentException(
						TypeName.simple(RetroArchive.class) + " must be present on exactly one type, but found on "
								+ annotatedType.getName() + " and " + type.getName());
			}
			annotatedType = type;
			archive = candidate;
		}

		if (archive == null) {
			throw new IllegalArgumentException(
					"Missing " + TypeName.simple(RetroArchive.class) + " on provided types.");
		}

		return archive;
	}
}
