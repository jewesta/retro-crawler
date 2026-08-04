package com.retrocrawler.core;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.CrawlPlanning;
import com.retrocrawler.core.archive.ReindexScope;
import com.retrocrawler.core.archive.Repository;
import com.retrocrawler.core.archive.source.ArchiveFileAccessor;
import com.retrocrawler.core.archive.source.ArchiveSource;
import com.retrocrawler.core.gear.FlatListFactory;
import com.retrocrawler.core.gear.GearTreeFactory;
import com.retrocrawler.core.progress.Progressor;
import com.retrocrawler.core.stash.Stash;
import com.retrocrawler.core.stash.StashFactory;

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
		 * Configures where extracted clue archives are stowed away and
		 * retrieved.
		 */
		Builder repository(Repository repository);

		/**
		 * Configures the bounded analysis sweep used to create approximate
		 * crawl regions. Defaults are used when omitted.
		 */
		Builder crawlPlanning(CrawlPlanning planning);

		/**
		 * Configures the provider used to inspect every archive root. The NIO
		 * filesystem source is used when omitted.
		 */
		Builder archiveSource(ArchiveSource source);

		/**
		 * Validates the required composition and creates the crawler.
		 */
		RetroCrawler build();
	}

	ArchiveDescriptor archiveDescriptor();

	/**
	 * Synchronously inspects the content of a file at one of this crawler's
	 * source addresses.
	 * <p>
	 * The selected archive source opens and closes both the session and the
	 * content stream. The inspector must neither close nor retain the supplied
	 * stream. An empty result means that the source recognizes the file but
	 * does not expose its content.
	 *
	 * @throws NoSuchFileException
	 *             if the address does not identify a file in the archive source
	 * @throws IllegalArgumentException
	 *             if the address is outside this crawler's configured archive
	 *             roots
	 */
	<T> Optional<T> inspect(Path sourcePath, ArchiveFileAccessor<T> inspector) throws IOException;

	<R, N, G> R crawl(Progressor progressor, ReindexScope reindexScope, GearTreeFactory<R, N, G> factory)
			throws IOException;

	/**
	 * Convenience method that builds a hierarchical {@link Stash} for the given
	 * gear type.
	 */
	default <G> Stash<G> crawlStash(final Progressor progressor, final ReindexScope reindexScope,
			final Class<G> gearType) throws IOException {
		return crawl(progressor, reindexScope, new StashFactory<>(gearType));
	}

	/**
	 * Convenience method that returns a flat list of all matching gear across
	 * all buckets (legacy behavior).
	 */
	default <G> List<G> crawlGear(final Progressor progressor, final ReindexScope reindexScope, final Class<G> gearType)
			throws IOException {
		return crawl(progressor, reindexScope, new FlatListFactory<>(gearType));
	}

}
