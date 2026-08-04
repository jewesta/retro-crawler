package com.retrocrawler.core.archive;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Selects which part of a stored clue archive should be rebuilt from its
 * configured source.
 */
public final class ReindexScope {

	public enum Kind {
		NONE,
		ALL,
		SUBTREES
	}

	private static final ReindexScope NONE = new ReindexScope(Kind.NONE, List.of());

	private static final ReindexScope ALL = new ReindexScope(Kind.ALL, List.of());

	private final Kind kind;

	private final List<Path> paths;

	private ReindexScope(final Kind kind, final Collection<Path> paths) {
		this.kind = Objects.requireNonNull(kind, "kind");
		this.paths = List.copyOf(Objects.requireNonNull(paths, "paths"));
	}

	/**
	 * Reuses the stored archive when one is available.
	 */
	public static ReindexScope none() {
		return NONE;
	}

	/**
	 * Rebuilds the complete archive from all configured roots.
	 */
	public static ReindexScope all() {
		return ALL;
	}

	/**
	 * Rebuilds one folder and everything below it.
	 */
	public static ReindexScope subtree(final Path path) {
		return subtrees(List.of(Objects.requireNonNull(path, "path")));
	}

	/**
	 * Rebuilds the supplied folders and everything below them.
	 */
	public static ReindexScope subtrees(final Path... paths) {
		Objects.requireNonNull(paths, "paths");
		return subtrees(Arrays.asList(paths));
	}

	/**
	 * Rebuilds the supplied folders and everything below them.
	 */
	public static ReindexScope subtrees(final Collection<Path> paths) {
		final List<Path> immutablePaths = List.copyOf(Objects.requireNonNull(paths, "paths"));
		if (immutablePaths.isEmpty()) {
			throw new IllegalArgumentException("At least one archive subtree is required.");
		}
		return new ReindexScope(Kind.SUBTREES, immutablePaths);
	}

	public Kind kind() {
		return kind;
	}

	public List<Path> paths() {
		return paths;
	}

	@Override
	public String toString() {
		return kind == Kind.SUBTREES ? getClass().getSimpleName() + paths : getClass().getSimpleName() + "." + kind;
	}
}
