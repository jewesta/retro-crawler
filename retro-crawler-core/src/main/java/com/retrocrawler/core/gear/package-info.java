/**
 * Model-dependent fact resolution and construction of collector-facing Gear.
 *
 * <p>
 * Resolution classifies clues, parses typed facts while retaining their source
 * evidence, selects a matching Gear definition, and emits resolved objects for
 * the collection Stash. Matcher and parser interfaces are the main consumer
 * extension points; resolver and descriptor types support the framework's
 * reflective implementation.
 */
package com.retrocrawler.core.gear;
