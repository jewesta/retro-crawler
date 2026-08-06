/**
 * Observable progress, cancellation, and completion reporting.
 *
 * <p>
 * The package retains the PEPPER 2 division between a read-only
 * {@link com.retrocrawler.core.progress.ProgressSupplier}, a driving
 * {@link com.retrocrawler.core.progress.ProgressController}, and the combined
 * {@link com.retrocrawler.core.progress.Progressor}. The implementation is a
 * dependency-free adaptation used with permission: translatable text, framework
 * logging, dependency injection, and external collection utilities are
 * deliberately absent.
 *
 * <p>
 * RetroCrawler adds structured stages, accuracy, work units, cancellation, and
 * terminal state. Monitors receive immutable snapshots while a crawl moves
 * through planning, digging, persistence, and resolution.
 */
package com.retrocrawler.core.progress;
