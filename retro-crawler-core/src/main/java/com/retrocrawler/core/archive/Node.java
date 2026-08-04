package com.retrocrawler.core.archive;

import java.nio.file.Path;
import java.util.Objects;

/**
 * The located archive node currently being interpreted.
 */
public record Node(Path archiveRoot, Path path) {

	public Node {
		archiveRoot = Objects.requireNonNull(archiveRoot, "archiveRoot");
		path = Objects.requireNonNull(path, "path");
		if (!path.normalize().startsWith(archiveRoot.normalize())) {
			throw new IllegalArgumentException("Node path must be below its archive root.");
		}
	}
}
