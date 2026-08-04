/**
 * Filesystem archive descriptions, digging, crawl planning, and repositories.
 *
 * <p>
 * This package owns the model-independent side of a crawl. An archive is dug
 * according to an {@link com.retrocrawler.core.archive.ArchiveDefinition} and
 * stored through a {@link com.retrocrawler.core.archive.Repository} so future
 * model or parser changes can reinterpret the same observed clues without
 * revisiting the filesystem. The filesystem archive remains the source of truth
 * and repository state remains rebuildable.
 */
package com.retrocrawler.core.archive;
