package com.retrocrawler.core.archive;

import java.nio.file.Path;
import java.util.Objects;

record ArchiveDigTarget(Path root, Path path) {

	ArchiveDigTarget {
		Objects.requireNonNull(root, "root");
		Objects.requireNonNull(path, "path");
		if (!path.startsWith(root)) {
			throw new IllegalArgumentException("Expected archive path '" + path + "' to be below root '" + root + "'.");
		}
	}
}
