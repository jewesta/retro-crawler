/**
 * Model-dependent fact resolution and construction of collector-facing Gear.
 *
 * <p>
 * Resolution classifies clues, parses typed facts while retaining their source
 * evidence, selects a matching Gear definition, and emits resolved objects
 * through a {@link com.retrocrawler.core.gear.GearTreeFactory}. Matcher,
 * parser, and result-factory interfaces are the main consumer extension points;
 * resolver and descriptor types support the framework's reflective
 * implementation.
 */
package com.retrocrawler.core.gear;
