package com.retrocrawler.core.archive;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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
		this.paths = List.copyOf(Objects.requireNonNull(paths, "paths"));
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
		Objects.requireNonNull(archive, "archive");
		final Collection<Path> paths = Arrays.stream(archive.locations()).map(String::trim).map(Path::of).toList();
		return fromPaths(archive, paths);
	}

	public static final ArchiveDescriptor of(final RetroArchive archive, final ArchiveRoots archiveRoots) {
		Objects.requireNonNull(archiveRoots, "archiveRoots");
		return fromPaths(archive, archiveRoots.getPaths());
	}

	private static ArchiveDescriptor fromPaths(final RetroArchive archive, final Collection<Path> paths) {
		Objects.requireNonNull(archive, "archive");
		final List<Path> immutablePaths = List.copyOf(Objects.requireNonNull(paths, "paths"));
		final ArchiveId id = ArchiveId.of(archive.id());
		final String name = archive.name().isBlank() ? id.get() : archive.name().trim();
		if (immutablePaths.isEmpty()) {
			throw new IllegalArgumentException(
					"A " + TypeName.simple(RetroArchive.class) + " requires at least one location to be set.");
		}
		return new ArchiveDescriptor(id, name, immutablePaths);
	}

}
