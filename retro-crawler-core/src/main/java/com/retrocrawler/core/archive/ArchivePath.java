package com.retrocrawler.core.archive;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * One filesystem folder being inspected, with the root that defines portable
 * archive-relative paths.
 */
public record ArchivePath(Path root, Path path, List<Path> children) {

	public ArchivePath {
		Objects.requireNonNull(root, "root");
		Objects.requireNonNull(path, "path");
		children = List.copyOf(Objects.requireNonNull(children, "children"));
		if (!path.normalize().startsWith(root.normalize())) {
			throw new IllegalArgumentException("Expected archive path '" + path + "' to be below root '" + root + "'.");
		}
	}

	/**
	 * Compatibility constructor for inspecting one folder as its own archive root.
	 */
	public ArchivePath(final Path path, final List<Path> children) {
		this(path, path, children);
	}

	public Path relative(final Path child) {
		Objects.requireNonNull(child, "child");
		final Path relative = root.normalize().relativize(child.normalize());
		if (relative.startsWith("..")) {
			throw new IllegalArgumentException("Expected archive entry '" + child + "' to be below root '" + root + "'.");
		}
		return relative;
	}

}
