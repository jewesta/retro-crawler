package com.retrocrawler.core.archive.source;

import java.io.IOException;
import java.io.InputStream;

/**
 * Synchronously inspects archive file content while its owning session keeps
 * the stream open.
 *
 * @param <T>
 *            inspection result type
 */
@FunctionalInterface
public interface ArchiveFileAccessor<T> {

	T access(InputStream content) throws IOException;
}
