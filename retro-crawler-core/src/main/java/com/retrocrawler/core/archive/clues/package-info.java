/**
 * Model-independent evidence observed while digging an archive.
 *
 * <p>
 * {@link com.retrocrawler.core.archive.clues.ClueFinder clue finders} inspect
 * names, selected file contents, or tree context and produce
 * {@link com.retrocrawler.core.archive.clues.Clue clues}. Artifacts and archive
 * nodes retain those observations and their structure; they never contain
 * model-dependent facts. Finder interfaces are extension points, while archive
 * value types form the traceable repository representation.
 */
package com.retrocrawler.core.archive.clues;
