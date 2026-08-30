/**
 * Model-independent evidence observed while digging an archive.
 *
 * <p>
 * {@link com.retrocrawler.core.archive.clues.ClueFinder clue finders} inspect a
 * transient, pruned folder view and produce
 * {@link com.retrocrawler.core.archive.clues.Clue clues}. Artifacts and archive
 * nodes retain those observations, their finder identity, optional source ARIs,
 * and their structure; they never contain model-dependent facts. The finder is
 * the extension point, while archive value types form the traceable repository
 * representation.
 */
package com.retrocrawler.core.archive.clues;
