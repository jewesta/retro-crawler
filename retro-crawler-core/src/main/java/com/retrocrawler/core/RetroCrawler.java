package com.retrocrawler.core;

import java.io.IOException;
import java.util.List;

import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.CrawlPlanning;
import com.retrocrawler.core.archive.ReindexScope;
import com.retrocrawler.core.archive.Repository;
import com.retrocrawler.core.progress.Progressor;

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
		 * Configures the bounded analysis sweep used to create approximate crawl
		 * regions. Defaults are used when omitted.
		 */
		Builder crawlPlanning(CrawlPlanning planning);

		/**
		 * Validates the required composition and creates the crawler.
		 */
		RetroCrawler build();
	}

	ArchiveDescriptor getArchiveDescriptor();

	<R, N, G> R crawl(Progressor progressor, ReindexScope reindexScope, GearTreeFactory<R, N, G> factory)
			throws IOException;

	/**
	 * Convenience method that builds a hierarchical {@link GearArchive} for the
	 * given gear type.
	 */
	default <G> GearArchive<G> crawlArchive(final Progressor progressor, final ReindexScope reindexScope,
			final Class<G> gearType) throws IOException {
		return crawl(progressor, reindexScope, new GearArchiveFactory<>(gearType));
	}

	/**
	 * Convenience method that returns a flat list of all matching gear across all
	 * buckets (legacy behavior).
	 */
	default <G> List<G> crawlGear(final Progressor progressor, final ReindexScope reindexScope,
			final Class<G> gearType) throws IOException {
		return crawl(progressor, reindexScope, new FlatListFactory<>(gearType));
	}

}
