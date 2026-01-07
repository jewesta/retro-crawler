package com.retrocrawler.core;

import java.util.List;
import java.util.Objects;

public final class GearArchive<G> {

	private final List<GearBucket<G>> buckets;

	public GearArchive(final List<GearBucket<G>> buckets) {
		this.buckets = List.copyOf(Objects.requireNonNull(buckets, "buckets"));
	}

	public List<GearBucket<G>> getBuckets() {
		return buckets;
	}
}