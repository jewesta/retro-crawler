/**
 * Filters that keep irrelevant filesystem paths out of an archive dig.
 *
 * <p>
 * {@link com.retrocrawler.core.archive.filter.ArchivePathFilter} is the
 * extension point for collection-specific path admission. The supplied filters
 * reject common operating-system and NAS housekeeping paths and can be composed
 * through crawl configuration.
 */
package com.retrocrawler.core.archive.filter;
