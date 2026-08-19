package com.retrocrawler.core;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.List;
import java.util.Optional;

import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.archive.CrawlPlanning;
import com.retrocrawler.core.archive.ReindexScope;
import com.retrocrawler.core.archive.Repository;
import com.retrocrawler.core.archive.source.ArchiveFileAccessor;
import com.retrocrawler.core.archive.source.ArchiveSource;
import com.retrocrawler.core.gear.FlatListFactory;
import com.retrocrawler.core.gear.GearTreeFactory;
import com.retrocrawler.core.stash.Stash;
import com.retrocrawler.core.stash.StashFactory;

/**
 * Applies one shared model to the archives registered with it.
 * <p>
 * Every archive has exactly one root, its own source provider, its own
 * repository entry, and its own crawl lifecycle. Aggregation across archives is
 * a crawler operation: {@code crawlAll} resolves every registered archive in
 * one pass and validates Retro ID uniqueness across all of them.
 */
public interface RetroCrawler {

	/**
	 * Starts explicit composition of a crawler from a model, a repository, and
	 * at least one archive.
	 */
	static Builder builder() {
		return new DefaultRetroCrawlerBuilder();
	}

	interface Builder {

		/**
		 * Configures the annotation-derived collection model shared by every
		 * registered archive.
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
		 * Registers an archive that is exposed by the NIO filesystem source.
		 */
		Builder archive(ArchiveDescriptor archive);

		/**
		 * Registers an archive and the provider that exposes its root. Archive
		 * IDs must be unique within one crawler.
		 */
		Builder archive(ArchiveDescriptor archive, ArchiveSource source);

		/**
		 * Validates the required composition and creates the crawler.
		 */
		RetroCrawler build();
	}

	/** All archives registered with this crawler, in composition order. */
	List<ArchiveDescriptor> archives();

	/**
	 * The collection namespace shared by every ARI produced by this crawler.
	 */
	String collectionId();

	/**
	 * Returns the registered archive with the given identity.
	 *
	 * @throws IllegalArgumentException
	 *             if no such archive is registered
	 */
	ArchiveDescriptor archive(ArchiveId archiveId);

	/**
	 * Synchronously inspects the content of an archive file.
	 * <p>
	 * The accessor receives an open stream that is closed as soon as it
	 * returns. Escaping streams must not be retained.
	 * <p>
	 * Due to a bug in OpenRewrite the return tag must come after the throws
	 * tags. Do not reorder them.
	 *
	 * @throws NoSuchFileException
	 *             if the archive source has no file at the ARI
	 * @throws IllegalArgumentException
	 *             if the ARI belongs to another collection or an unknown
	 *             archive
	 * @return the inspected value, or an empty optional if the archive source
	 *         cannot expose the file content
	 */
	<T> Optional<T> inspect(ARI source, ArchiveFileAccessor<T> inspector) throws IOException;

	/** Crawls and resolves the selected archive through the shared model. */
	<R, N, G> R crawl(ArchiveId archiveId, Journal journal, ReindexScope reindexScope, GearTreeFactory<R, N, G> factory)
			throws IOException;

	/**
	 * Crawls and resolves every registered archive in one pass.
	 * <p>
	 * Retro ID uniqueness is validated across all archives. A subtree reindex
	 * scope is routed to the archive identified by each requested ARI; archives
	 * without a requested subtree reuse their stored clue archive.
	 */
	<R, N, G> R crawlAll(Journal journal, ReindexScope reindexScope, GearTreeFactory<R, N, G> factory)
			throws IOException;

	/**
	 * Convenience method that builds a hierarchical {@link Stash} from the
	 * selected archive.
	 */
	default <G> Stash<G> crawlStash(final ArchiveId archiveId, final Journal journal, final ReindexScope reindexScope,
			final Class<G> gearType) throws IOException {
		return crawl(archiveId, journal, reindexScope, new StashFactory<>(gearType));
	}

	/**
	 * Convenience method that builds one hierarchical {@link Stash} across
	 * every registered archive.
	 */
	default <G> Stash<G> crawlAllStash(final Journal journal, final ReindexScope reindexScope, final Class<G> gearType)
			throws IOException {
		return crawlAll(journal, reindexScope, new StashFactory<>(gearType));
	}

	/**
	 * Convenience method that returns matching gear from the selected archive
	 * as a flat list.
	 */
	default <G> List<G> crawlGear(final ArchiveId archiveId, final Journal journal, final ReindexScope reindexScope,
			final Class<G> gearType) throws IOException {
		return crawl(archiveId, journal, reindexScope, new FlatListFactory<>(gearType));
	}

	/**
	 * Convenience method that returns matching gear from every registered
	 * archive as one flat list.
	 */
	default <G> List<G> crawlAllGear(final Journal journal, final ReindexScope reindexScope, final Class<G> gearType)
			throws IOException {
		return crawlAll(journal, reindexScope, new FlatListFactory<>(gearType));
	}

}
