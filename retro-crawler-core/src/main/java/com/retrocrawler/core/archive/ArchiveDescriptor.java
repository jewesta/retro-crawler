package com.retrocrawler.core.archive;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

import com.retrocrawler.core.annotation.RetroArchive;
import com.retrocrawler.core.util.Descriptor;
import com.retrocrawler.core.util.TypeName;

public class ArchiveDescriptor implements Descriptor {

	private final ArchiveId id;

	private final String name;

	private final Collection<Path> paths;

	public ArchiveDescriptor(final ArchiveId id, final String name, final Collection<Path> paths) {
		this.id = Objects.requireNonNull(id, "id");
		this.name = Objects.requireNonNull(name, "name");
		this.paths = Objects.requireNonNull(paths, "paths");
	}

	public ArchiveId getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Collection<Path> getPaths() {
		return paths;
	}

	public static final ArchiveDescriptor valueOf(final ArchiveId id, final String name,
			final Collection<String> pathNames) {
		final Collection<Path> paths = pathNames.stream().map(Path::of).toList();
		return new ArchiveDescriptor(id, name, paths);
	}

	public static final ArchiveDescriptor of(final RetroArchive archive) {
		final ArchiveId id = ArchiveId.of(archive.id());
		final String name = archive.name().isBlank() ? id.get() : archive.name().trim();
		final Collection<String> pathNames = Arrays.stream(archive.locations()).map(String::trim).toList();
		if (pathNames.isEmpty()) {
			throw new IllegalArgumentException(
					"A " + TypeName.simple(RetroArchive.class) + " requires at least one location to be set.");
		}
		return valueOf(id, name, pathNames);
	}

}
