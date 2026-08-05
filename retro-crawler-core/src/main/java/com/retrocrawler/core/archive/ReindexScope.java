package com.retrocrawler.core.archive;

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

	private final List<ARI> subtrees;

	private ReindexScope(final Kind kind, final Collection<ARI> subtrees) {
		this.kind = Objects.requireNonNull(kind, "kind");
		this.subtrees = List.copyOf(Objects.requireNonNull(subtrees, "subtrees"));
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
	 * Rebuilds one identified folder and everything below it.
	 */
	public static ReindexScope subtree(final ARI subtree) {
		return subtrees(List.of(Objects.requireNonNull(subtree, "subtree")));
	}

	/**
	 * Rebuilds the supplied identified folders and everything below them.
	 */
	public static ReindexScope subtrees(final ARI... subtrees) {
		Objects.requireNonNull(subtrees, "subtrees");
		return subtrees(Arrays.asList(subtrees));
	}

	/**
	 * Rebuilds the supplied identified folders and everything below them.
	 */
	public static ReindexScope subtrees(final Collection<ARI> subtrees) {
		final List<ARI> immutableSubtrees = List.copyOf(Objects.requireNonNull(subtrees, "subtrees"));
		if (immutableSubtrees.isEmpty()) {
			throw new IllegalArgumentException("At least one archive subtree is required.");
		}
		return new ReindexScope(Kind.SUBTREES, immutableSubtrees);
	}

	public Kind kind() {
		return kind;
	}

	public List<ARI> subtrees() {
		return subtrees;
	}

	@Override
	public String toString() {
		return kind == Kind.SUBTREES ? getClass().getSimpleName() + subtrees : getClass().getSimpleName() + "." + kind;
	}
}
