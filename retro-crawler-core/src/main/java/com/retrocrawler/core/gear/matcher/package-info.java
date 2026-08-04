/**
 * Strategies for rating whether observed evidence represents a kind of Gear.
 *
 * <p>
 * Collection models implement
 * {@link com.retrocrawler.core.gear.matcher.GearMatcher} when Gear selection
 * needs domain-specific evidence. Matchers inspect a resolution context and
 * report confidence; they do not crawl archives or mutate clues.
 */
package com.retrocrawler.core.gear.matcher;
