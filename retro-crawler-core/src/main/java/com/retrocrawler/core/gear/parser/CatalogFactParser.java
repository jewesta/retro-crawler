package com.retrocrawler.core.gear.parser;

import com.retrocrawler.core.catalog.Catalog;

/**
 * A fact parser whose interpretation is backed by one standard catalog.
 */
public interface CatalogFactParser<K extends Enum<K>, T> extends FactParser<T> {

	Catalog<K> catalog();
}
