package com.retrocrawler.core.archive.clues;

import java.util.List;

/**
 * Read-only, transient view of an archive folder during clue discovery.
 * <p>
 * This is the complete boundary presented to a {@link ClueFinder} while the
 * root folder is classified. {@link #files()} contains its direct files.
 * {@link #folders()} contains only child folders that were positively
 * established as clue-free metadata folders. The view is consequently pruned at
 * artifact and failure boundaries, while the persistent archive tree retains
 * every folder.
 */
public interface ArchiveFolderView {

	String name();

	List<ArchiveFolderView> folders();

	List<ArchiveFileView> files();

}
