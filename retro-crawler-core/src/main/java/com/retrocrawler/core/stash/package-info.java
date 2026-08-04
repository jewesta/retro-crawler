/**
 * Model-dependent stashes of resolved Gear and their hierarchical structure.
 *
 * <p>
 * A stash is derived by interpreting a model-independent
 * {@link com.retrocrawler.core.archive.clues.Archive clue archive} through a
 * {@link com.retrocrawler.core.Model model}. The clue archive remains the
 * rebuildable repository representation and the configured archive source
 * remains the source of truth. This package owns the collector-facing result
 * and is the natural home for its future structured exploration and query
 * capabilities.
 */
package com.retrocrawler.core.stash;
