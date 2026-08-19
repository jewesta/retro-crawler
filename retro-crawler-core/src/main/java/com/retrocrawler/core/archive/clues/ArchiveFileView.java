package com.retrocrawler.core.archive.clues;

import java.io.InputStream;
import java.util.Optional;
import java.util.function.Function;

/**
 * Read-only, crawl-time view of a file in an {@link ArchiveFolderView}.
 * <p>
 * The file content is opened only when {@link #peek(Function)} is called. The
 * crawler owns and closes the supplied stream after the inspector returns.
 */
public interface ArchiveFileView {

	String name();

	/**
	 * Inspects this file when its archive source exposes content.
	 * <p>
	 * The supplied stream is valid only for the duration of the inspector call
	 * and must not be closed or retained by the inspector.
	 *
	 * @return the non-null inspection result, or empty when content is
	 *         unavailable
	 */
	<T> Optional<T> peek(Function<? super InputStream, ? extends T> inspector);

}
