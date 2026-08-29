package com.retrocrawler.core.stash;

import java.util.List;
import java.util.Objects;

/**
 * The complete immutable, model-dependent arrangement of resolved Gear.
 *
 * <p>
 * A stash is derived from the model-independent clue archives. It is a runtime
 * view rather than the collection's source of truth. Its gear stays grouped by
 * the archive it was found in, so every result keeps its provenance.
 */
public final class Stash {

	private final List<ArchiveGear<Object>> archives;

	public Stash(final List<ArchiveGear<Object>> archives) {
		this.archives = List.copyOf(Objects.requireNonNull(archives, "archives"));
	}

	/** Every archive and its natural Gear hierarchy, in composition order. */
	public List<ArchiveGear<Object>> archives() {
		return archives;
	}

	/** Pulls every recognized Gear occurrence from this Stash. */
	public Batch<Object> pull() {
		return query(Object.class).pull();
	}

	/**
	 * Starts an immutable query selecting occurrences assignable to the type.
	 */
	public <G> Query<G> query(final Class<G> gearType) {
		return Query.from(this, gearType);
	}

}
