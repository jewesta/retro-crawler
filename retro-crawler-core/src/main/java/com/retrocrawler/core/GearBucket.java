package com.retrocrawler.core;

import java.util.List;
import java.util.Objects;

import com.retrocrawler.core.archive.clues.Bucket;

public final class GearBucket<G> {

	private final Bucket bucket;

	private final List<GearNode<G>> roots;

	public GearBucket(final Bucket bucket, final List<GearNode<G>> roots) {
		this.bucket = Objects.requireNonNull(bucket, "bucket");
		this.roots = List.copyOf(Objects.requireNonNull(roots, "roots"));
	}

	public Bucket getBucket() {
		return bucket;
	}

	public List<GearNode<G>> getRoots() {
		return roots;
	}
}