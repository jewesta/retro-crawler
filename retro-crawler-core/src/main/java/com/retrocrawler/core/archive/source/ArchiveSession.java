package com.retrocrawler.core.archive.source;

import java.io.IOException;
import java.util.Optional;

/**
 * One rooted, read-only session with an archive source.
 * <p>
 * Folder and file handles belong to the session that produced them. All file
 * content streams are created and closed by the session. Accessors must not
 * close or retain the supplied stream. Listings must describe unique direct
 * children and must form an acyclic tree below {@link #root()}.
 */
public interface ArchiveSession extends AutoCloseable {

	ArchiveFolder root() throws IOException;

	ArchiveListing list(ArchiveFolder folder) throws IOException;

	/**
	 * Inspects file content when the source makes it available.
	 * <p>
	 * An empty result means that content access is deliberately unavailable for
	 * this file and the accessor was not invoked. Access failures are reported
	 * as {@link IOException}. A successful accessor must return a non-null
	 * result.
	 */
	<T> Optional<T> access(ArchiveFile file, ArchiveFileAccessor<T> accessor) throws IOException;

	@Override
	void close() throws IOException;
}
