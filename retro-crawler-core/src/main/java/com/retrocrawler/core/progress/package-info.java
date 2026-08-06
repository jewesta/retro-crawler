/**
 * Observable progress, cancellation, and completion reporting.
 *
 * <p>
 * A {@link com.retrocrawler.core.progress.Progressor} publishes immutable
 * snapshots to a {@link com.retrocrawler.core.progress.ProgressMonitor} while a
 * crawl moves through planning, digging, persistence, and resolution stages.
 * Monitors are consumer extension points and may request cooperative
 * cancellation.
 */
package com.retrocrawler.core.progress;
