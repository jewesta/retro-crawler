package com.retrocrawler.core;

import com.retrocrawler.core.archive.clues.Bucket;

/**
 * @param <R> result type (e.g., TreeData, List, GearArchive, etc.)
 * @param <N> node handle used internally by the factory
 * @param <G> gear element type
 */
public interface GearTreeFactory<R, N, G> {

	/**
	 * The desired gear type this factory wants to receive.
	 *
	 * RetroCrawler will ignore any resolved gear instance that is not an instance
	 * of this type.
	 */
	Class<G> gearType();

	/**
	 * Called once per bucket before any nodes are emitted.
	 */
	void beginBucket(Bucket bucket);

	/**
	 * Called once per bucket after all nodes are emitted.
	 */
	void endBucket(Bucket bucket);

	/**
	 * Called for every produced gear node (compressed tree).
	 *
	 * @param parent the parent node handle, or null if this is a root in its bucket
	 * @param gear   the resolved gear instance
	 * @return a node handle that will be passed as parent for its children
	 */
	N addNode(N parent, G gear);

	/**
	 * Called after the whole crawl.
	 */
	R build();
}