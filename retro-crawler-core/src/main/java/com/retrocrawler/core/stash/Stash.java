package com.retrocrawler.core.stash;

import java.util.List;
import java.util.Objects;

/**
 * An immutable, model-dependent arrangement of resolved gear.
 *
 * <p>
 * A stash is derived from the model-independent clue archives. It is a runtime
 * view rather than the collection's source of truth. Its gear stays grouped by
 * the archive it was found in, so every result keeps its provenance.
 */
public final class Stash<G> {

	private final List<ArchiveGear<G>> archives;

	public Stash(final List<ArchiveGear<G>> archives) {
		this.archives = List.copyOf(Objects.requireNonNull(archives, "archives"));
	}

	public List<ArchiveGear<G>> archives() {
		return archives;
	}

}
