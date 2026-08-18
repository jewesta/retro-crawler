package com.retrocrawler.core.archive;

import java.nio.file.Path;
import java.util.Objects;

/** Runtime location of the artifact currently being interpreted. */
public record ArtifactLocation(Path archiveRoot, Path sourcePath) {

	public ArtifactLocation {
		archiveRoot = Objects.requireNonNull(archiveRoot, "archiveRoot");
		sourcePath = Objects.requireNonNull(sourcePath, "sourcePath");
		if (!sourcePath.normalize().startsWith(archiveRoot.normalize())) {
			throw new IllegalArgumentException("Artifact source path must be below its archive root.");
		}
	}
}
