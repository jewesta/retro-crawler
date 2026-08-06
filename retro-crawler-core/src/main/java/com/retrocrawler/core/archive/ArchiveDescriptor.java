package com.retrocrawler.core.archive;

import java.nio.file.Path;
import java.util.Objects;

import com.retrocrawler.core.util.Descriptor;

/**
 * One separately identified archive and the single hierarchical root that holds
 * it.
 * <p>
 * An archive has exactly one root. A collection spread across several
 * locations, providers, or media is composed as several archives on one
 * crawler, each with its own identity, source, and repository entry.
 */
public record ArchiveDescriptor(ArchiveId id, String name, Path root) implements Descriptor {

	public ArchiveDescriptor {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(name, "name");
		Objects.requireNonNull(root, "root");
	}

	/**
	 * Describes an archive that uses its identity as its display name.
	 */
	public static ArchiveDescriptor of(final ArchiveId id, final Path root) {
		return new ArchiveDescriptor(Objects.requireNonNull(id, "id"), id.value(), root);
	}

	public static ArchiveDescriptor valueOf(final String id, final String name, final String rootName) {
		Objects.requireNonNull(rootName, "rootName");
		return new ArchiveDescriptor(ArchiveId.of(id), name, Path.of(rootName));
	}

}
