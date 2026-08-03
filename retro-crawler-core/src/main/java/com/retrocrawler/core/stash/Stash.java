package com.retrocrawler.core.stash;

import java.util.List;
import java.util.Objects;

/**
 * An immutable, model-dependent arrangement of resolved gear.
 *
 * <p>
 * A stash is derived from the model-independent clue archive. It is a runtime
 * view rather than the collection's source of truth.
 */
public final class Stash<G> {

	private final List<GearBucket<G>> buckets;

	public Stash(final List<GearBucket<G>> buckets) {
		this.buckets = List.copyOf(Objects.requireNonNull(buckets, "buckets"));
	}

	public List<GearBucket<G>> buckets() {
		return buckets;
	}

}
