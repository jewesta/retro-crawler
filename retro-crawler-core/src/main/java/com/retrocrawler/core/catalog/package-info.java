/**
 * Strict external catalogs used by model-dependent fact parsers.
 *
 * <p>
 * A {@link com.retrocrawler.core.catalog.Catalog} is immutable, enum-keyed TSV
 * data. A {@link com.retrocrawler.core.catalog.CatalogLoader} locates bundled
 * or collection-supplied catalogs only when a selected parser needs them.
 * Catalog rows support interpretation but do not become part of the persisted
 * clue archive.
 */
package com.retrocrawler.core.catalog;
