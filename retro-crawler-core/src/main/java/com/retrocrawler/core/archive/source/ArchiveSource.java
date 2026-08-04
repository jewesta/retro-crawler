package com.retrocrawler.core.archive.source;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Opens read-only archive sessions for configured hierarchical roots.
 * <p>
 * The source interprets root paths. They are not assumed to belong to the local
 * filesystem.
 */
@FunctionalInterface
public interface ArchiveSource {

	ArchiveSession open(Path root) throws IOException;
}
