package com.retrocrawler.core.archive.clues;

import java.util.List;

/**
 * Read-only, transient view of an archive folder during clue discovery.
 * <p>
 * {@link #folders()} contains only child folders that did not establish an
 * {@link Artifact}. The view is consequently pruned at artifact boundaries,
 * while the persistent archive tree retains every folder.
 */
public interface ArchiveFolderView {

	String name();

	List<ArchiveFolderView> folders();

	List<ArchiveFileView> files();

}
