package com.retrocrawler.core.progress;

import java.time.Duration;
import java.util.Objects;

/** Creates a dependency-free, single-line description of progress. */
public final class ProgressMessageGenerator {

	private final ProgressSupplier progress;

	public ProgressMessageGenerator(final ProgressSupplier progress) {
		this.progress = Objects.requireNonNull(progress, "progress");
	}

	public String message(final boolean showTimeRemaining) {
		if (progress.millisElapsed() == 0) {
			return "Starting.";
		}
		final int percent = (int) Math.round(progress.progress() * 100);
		if (!showTimeRemaining || progress.remaining().isEmpty()) {
			return percent + "% complete.";
		}
		return percent + "% complete. Approximately " + compact(progress.remaining().orElseThrow()) + " remaining.";
	}

	private static String compact(final Duration duration) {
		final long seconds = Math.max(0, duration.toSeconds());
		if (seconds < 60) {
			return seconds + "s";
		}
		final long minutes = seconds / 60;
		if (minutes < 60) {
			return minutes + "m " + seconds % 60 + "s";
		}
		return minutes / 60 + "h " + minutes % 60 + "m";
	}
}
