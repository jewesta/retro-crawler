/**
 * Entry points for describing a collection model and crawling its archives.
 *
 * <p>
 * A {@link com.retrocrawler.core.Model} defines how model-independent clues
 * become collector-facing Gear. A configured
 * {@link com.retrocrawler.core.RetroCrawler} coordinates archive retrieval or
 * digging, gear resolution, and result construction. Callers normally enter
 * through the model and crawler builders rather than framework implementation
 * classes.
 */
package com.retrocrawler.core;
