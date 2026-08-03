package com.retrocrawler.core.catalog;

/**
 * Parser-scoped access to the catalog source selected for a model.
 */
@FunctionalInterface
public interface CatalogLoader {

	<K extends Enum<K>> Catalog<K> load(Class<K> keyType, String defaultCatalogFile);
}
