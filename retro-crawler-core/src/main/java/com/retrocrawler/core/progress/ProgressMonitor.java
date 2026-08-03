package com.retrocrawler.core.progress;

/**
 * Observes immutable progress snapshots.
 *
 * <p>
 * Adapted from progressor code developed by Relimit GmbH. Used in RetroCrawler
 * with permission.
 */
@FunctionalInterface
public interface ProgressMonitor {

	void onProgress(ProgressSnapshot progress);
}
