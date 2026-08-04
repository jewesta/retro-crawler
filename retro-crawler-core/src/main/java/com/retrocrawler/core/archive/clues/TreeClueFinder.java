package com.retrocrawler.core.archive.clues;

import java.util.Set;

/**
 * Extracts clues for the current archive folder from its transient, pruned
 * folder tree.
 * <p>
 * A tree finder may navigate metadata folders and inspect available file
 * content through {@link ArchiveFileView#peek(java.util.function.Function)}. It
 * cannot cross a child artifact boundary or traverse the configured source
 * directly through this API. Every returned clue belongs to the current folder
 * being examined.
 */
public interface TreeClueFinder extends ClueFinder {

	Set<Clue> find(ArchiveFolderView folder);

}
