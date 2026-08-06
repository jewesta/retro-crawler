package com.retrocrawler.core.gear;

import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.archive.ArchiveDescriptor;

/**
 * @param <R>
 *            result type (e.g., TreeData, List, Stash, etc.)
 * @param <N>
 *            node handle used internally by the factory
 * @param <G>
 *            gear element type
 */
public interface GearTreeFactory<R, N, G> {

	/**
	 * The desired gear type this factory wants to receive.
	 *
	 * RetroCrawler will ignore any resolved gear instance that is not an
	 * instance of this type.
	 */
	Class<G> gearType();

	/**
	 * Called once per archive before any of its nodes are emitted.
	 */
	void beginArchive(ArchiveDescriptor archive);

	/**
	 * Called once per archive after all of its nodes are emitted.
	 */
	void endArchive(ArchiveDescriptor archive);

	/**
	 * Called for every produced gear node (compressed tree).
	 *
	 * @param parent
	 *            the parent node handle, or null if this is a root in its
	 *            archive
	 * @param gear
	 *            the resolved gear instance
	 * @return a node handle that will be passed as parent for its children
	 */
	N addNode(N parent, G gear);

	/**
	 * Called for every produced gear node together with its source.
	 * <p>
	 * The default implementation preserves compatibility with factories that do
	 * not need the source.
	 *
	 * @param parent
	 *            the parent node handle, or null if this is a root in its
	 *            archive
	 * @param gear
	 *            the resolved gear instance
	 * @param source
	 *            the ARI of the artifact that produced the gear
	 * @return a node handle that will be passed as parent for its children
	 */
	default N addNode(final N parent, final G gear, final ARI source) {
		return addNode(parent, gear);
	}

	/**
	 * Called after the whole crawl.
	 */
	R build();
}
