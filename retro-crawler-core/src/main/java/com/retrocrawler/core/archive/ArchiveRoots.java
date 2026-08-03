package com.retrocrawler.core.archive;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Supplies archive roots to a model. Factory methods create immutable
 * implementations, including from a plain UTF-8 file containing one path per
 * line. Root order is significant: stored buckets are rebound to current
 * deployment paths in the same order when a clue archive is reused.
 */
@FunctionalInterface
public interface ArchiveRoots {

	Collection<Path> paths();

	static ArchiveRoots from(final Path... rootPaths) {
		Objects.requireNonNull(rootPaths, "rootPaths");
		return from(Arrays.asList(rootPaths));
	}

	static ArchiveRoots from(final Collection<Path> rootPaths) {
		final List<Path> roots = List.copyOf(Objects.requireNonNull(rootPaths, "rootPaths"));
		if (roots.isEmpty()) {
			throw new IllegalArgumentException("At least one archive root is required.");
		}
		return () -> roots;
	}

	static ArchiveRoots load(final Path pathFile) throws IOException {
		Objects.requireNonNull(pathFile, "pathFile");

		final List<String> lines = Files.readAllLines(pathFile, StandardCharsets.UTF_8);
		final List<Path> roots = new ArrayList<>();
		for (int index = 0; index < lines.size(); index++) {
			final String line = lines.get(index).trim();
			if (line.isEmpty()) {
				continue;
			}
			try {
				roots.add(Path.of(line));
			} catch (final InvalidPathException e) {
				throw new IllegalArgumentException(
						"Invalid archive root on line " + (index + 1) + " of " + pathFile + ": " + line, e);
			}
		}
		return from(roots);
	}
}
