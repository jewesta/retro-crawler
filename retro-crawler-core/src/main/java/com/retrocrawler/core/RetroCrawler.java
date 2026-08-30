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
import com.retrocrawler.core.gear.filter.FilterDefinition;
import com.retrocrawler.core.stash.Stash;

/**
 * Applies one shared model to the archives registered with it.
 * <p>
 * Every archive has exactly one root, its own source provider, its own
 * repository entry, and its own crawl lifecycle. Every operation resolves the
 * complete configured collection and validates Retro ID uniqueness across all
 * archives.
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

	/** Every structured filter defined by this crawler's immutable model. */
	List<FilterDefinition<?>> filters();

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

	/**
	 * Accesses the current immutable Stash.
	 * <p>
	 * A Stash already produced by this crawler is returned directly. Otherwise,
	 * stored clue archives are resolved and only archives without stored clues
	 * are physically crawled.
	 */
	Stash access(Journal journal) throws IOException;

	/**
	 * Physically crawls the requested scope and returns the resulting complete
	 * immutable Stash.
	 * <p>
	 * Archives and subtrees outside the requested scope reuse their stored
	 * clues. The new Stash becomes current only after the complete crawl and
	 * resolution succeeds.
	 */
	Stash crawl(Journal journal, ReindexScope reindexScope) throws IOException;

}
