package com.retrocrawler.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.retrocrawler.core.archive.clues.Bucket;

public final class GearArchiveFactory<G>
		implements GearTreeFactory<GearArchive<G>, GearArchiveFactory.MutableNode<G>, G> {

	private final Class<G> gearType;

	private final List<BucketBuild<G>> buckets = new ArrayList<>();

	private BucketBuild<G> currentBucket;

	public GearArchiveFactory(final Class<G> gearType) {
		this.gearType = Objects.requireNonNull(gearType, "gearType");
	}

	@Override
	public Class<G> gearType() {
		return gearType;
	}

	@Override
	public void beginBucket(final Bucket bucket) {
		Objects.requireNonNull(bucket, "bucket");
		if (currentBucket != null) {
			throw new IllegalStateException("beginBucket called while previous bucket is still open.");
		}
		currentBucket = new BucketBuild<>(bucket);
	}

	@Override
	public void endBucket(final Bucket bucket) {
		Objects.requireNonNull(bucket, "bucket");
		if (currentBucket == null) {
			throw new IllegalStateException("endBucket called without a matching beginBucket.");
		}
		if (!currentBucket.bucket.equals(bucket)) {
			throw new IllegalStateException("endBucket called with a different bucket than beginBucket.");
		}
		buckets.add(currentBucket);
		currentBucket = null;
	}

	@Override
	public MutableNode<G> addNode(final MutableNode<G> parent, final G gear) {
		Objects.requireNonNull(gear, "gear");
		if (currentBucket == null) {
			throw new IllegalStateException("addNode called outside of beginBucket/endBucket. Expected: "
					+ GearTreeFactory.class.getSimpleName());
		}

		final MutableNode<G> node = new MutableNode<>(gear);

		if (parent == null) {
			currentBucket.roots.add(node);
		} else {
			parent.children.add(node);
		}

		return node;
	}

	@Override
	public GearArchive<G> build() {
		if (currentBucket != null) {
			throw new IllegalStateException("build called while a bucket is still open.");
		}

		final List<GearBucket<G>> resultBuckets = new ArrayList<>();
		for (final BucketBuild<G> bucketBuild : buckets) {
			final List<GearNode<G>> roots = toImmutableNodes(bucketBuild.roots);
			resultBuckets.add(new GearBucket<>(bucketBuild.bucket, roots));
		}

		return new GearArchive<>(resultBuckets);
	}

	private static <G> List<GearNode<G>> toImmutableNodes(final List<MutableNode<G>> nodes) {
		if (nodes.isEmpty()) {
			return List.of();
		}
		final List<GearNode<G>> out = new ArrayList<>(nodes.size());
		for (final MutableNode<G> node : nodes) {
			out.add(toImmutableNode(node));
		}
		return List.copyOf(out);
	}

	private static <G> GearNode<G> toImmutableNode(final MutableNode<G> node) {
		final List<GearNode<G>> children = toImmutableNodes(node.children);
		return new GearNode<>(node.gear, children);
	}

	private static final class BucketBuild<G> {

		private final Bucket bucket;

		private final List<MutableNode<G>> roots = new ArrayList<>();

		private BucketBuild(final Bucket bucket) {
			this.bucket = bucket;
		}
	}

	/**
	 * Internal mutable node used as the factory handle type.
	 */
	public static final class MutableNode<G> {

		private final G gear;

		private final List<MutableNode<G>> children = new ArrayList<>();

		private MutableNode(final G gear) {
			this.gear = gear;
		}
	}
}