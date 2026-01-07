package com.retrocrawler.core;

import java.util.Objects;
import java.util.Set;

import com.retrocrawler.core.annotation.RetroArchive;
import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveDigger;
import com.retrocrawler.core.archive.clues.ArchivePathClueFinder;
import com.retrocrawler.core.gear.GearResolver;
import com.retrocrawler.core.gear.GearResolverFactory;
import com.retrocrawler.core.gear.ReflectiveFactory;
import com.retrocrawler.core.util.TypeName;

public class RetroCrawlerFactory implements ReflectiveFactory<RetroCrawler> {

	private static final GearResolverFactory GEAR_RESOLVER_FACTORY = new GearResolverFactory();

	@Override
	public RetroCrawler reflectOn(final Set<Class<?>> types) {
		Objects.requireNonNull(types, "types");

		final RetroArchive archive = assertRetroArchiveOnOneOf(types);
		final ArchiveDescriptor descriptor = ArchiveDescriptor.of(archive);

		final RetroArchive.LookAt lookAt = archive.findClues();
		final ArchivePathClueFinder clueFinder = ArchivePathClueFinder.of(lookAt);

		final ArchiveDigger digger = new ArchiveDigger(descriptor, clueFinder);

		final GearResolver gearResolver = GEAR_RESOLVER_FACTORY.reflectOn(types);

		return new RetroCrawlerImpl(descriptor, digger, gearResolver);
	}

	private static RetroArchive assertRetroArchiveOnOneOf(final Set<Class<?>> types) {
		RetroArchive archive = null;
		Class<?> annotatedType = null;

		for (final Class<?> type : types) {
			final RetroArchive a = type.getAnnotation(RetroArchive.class);
			if (a == null) {
				continue;
			}
			if (annotatedType != null) {
				throw new IllegalArgumentException(
						TypeName.simple(RetroArchive.class) + " must be present on exactly one type, but found on "
								+ annotatedType.getName() + " and " + type.getName());
			}
			annotatedType = type;
			archive = a;
		}

		if (archive == null) {
			throw new IllegalArgumentException(
					"Missing " + TypeName.simple(RetroArchive.class) + " on provided types.");
		}

		return archive;
	}
}