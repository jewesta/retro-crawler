package com.retrocrawler.core;

import java.io.IOException;
import java.util.List;

import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.util.Monitor;

public interface RetroCrawler {

	ArchiveDescriptor getArchiveDescriptor();

	<R, N, G> R find(Monitor monitor, boolean reindex, GearTreeFactory<R, N, G> factory) throws IOException;

	/**
	 * Convenience method that builds a hierarchical {@link GearArchive} for the
	 * given gear type.
	 */
	default <G> GearArchive<G> findArchive(final Monitor monitor, final boolean reindex, final Class<G> gearType)
			throws IOException {
		return find(monitor, reindex, new GearArchiveFactory<>(gearType));
	}

	/**
	 * Convenience method that returns a flat list of all matching gears across all
	 * buckets (legacy behavior).
	 */
	default <G> List<G> findAll(final Monitor monitor, final boolean reindex, final Class<G> gearType)
			throws IOException {
		return find(monitor, reindex, new FlatListFactory<>(gearType));
	}

}