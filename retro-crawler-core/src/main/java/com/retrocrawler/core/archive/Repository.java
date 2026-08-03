package com.retrocrawler.core.archive;

import java.util.Optional;

import com.retrocrawler.core.archive.clues.Archive;

/**
 * Stores extracted clue archives.
 * <p>
 * The configured filesystem archive remains RetroCrawler's source of truth. A
 * repository holds the rebuildable result of crawling that source.
 */
public interface Repository {

	/**
	 * Stores the given archive, replacing an archive with the same id.
	 *
	 * @throws RepositoryException
	 *             if the archive cannot be stored
	 */
	void stowaway(Archive archive);

	/**
	 * Retrieves the archive with the given id.
	 *
	 * @return the archive, or an empty optional if no such archive is stored
	 * @throws RepositoryException
	 *             if stored data exists but cannot be retrieved
	 */
	Optional<Archive> retrieve(ArchiveId id);

}
