/**
 * Parsers that turn raw clue values into typed, rated facts.
 *
 * <p>
 * {@link com.retrocrawler.core.gear.parser.FactParser} is the general extension
 * point. Standard parsers cover common scalar types, while catalog-backed
 * parsers can use external reference data and contextual parsers can inspect an
 * artifact's runtime archive location. Parsing happens during resolution and
 * leaves the cached clue archive unchanged.
 */
package com.retrocrawler.core.gear.parser;
