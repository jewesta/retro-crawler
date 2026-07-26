package com.retrocrawler.core;

import java.io.IOException;
import java.util.List;

import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.Repository;
import com.retrocrawler.core.util.Monitor;

public interface RetroCrawler {

	/**
	 * Starts explicit composition of a crawler from a model and repository.
	 */
	static Builder builder() {
		return new DefaultRetroCrawlerBuilder();
	}

	interface Builder {

		/**
		 * Configures the annotation-derived collection model.
		 */
		Builder model(Model model);

		/**
		 * Configures where extracted clue archives are stowed away and retrieved.
		 */
		Builder repository(Repository repository);

		/**
		 * Validates the required composition and creates the crawler.
		 */
		RetroCrawler build();
	}

	ArchiveDescriptor getArchiveDescriptor();

	<R, N, G> R crawl(Monitor monitor, boolean reindex, GearTreeFactory<R, N, G> factory) throws IOException;

	/**
	 * Convenience method that builds a hierarchical {@link GearArchive} for the
	 * given gear type.
	 */
	default <G> GearArchive<G> crawlArchive(final Monitor monitor, final boolean reindex, final Class<G> gearType)
			throws IOException {
		return crawl(monitor, reindex, new GearArchiveFactory<>(gearType));
	}

	/**
	 * Convenience method that returns a flat list of all matching gear across all
	 * buckets (legacy behavior).
	 */
	default <G> List<G> crawlGear(final Monitor monitor, final boolean reindex, final Class<G> gearType)
			throws IOException {
		return crawl(monitor, reindex, new FlatListFactory<>(gearType));
	}

}
