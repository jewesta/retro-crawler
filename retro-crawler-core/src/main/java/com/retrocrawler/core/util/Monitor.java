package com.retrocrawler.core.util;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class Monitor {

	private final Consumer<String> progressMessageConsumer;

	private final Consumer<CrawlProgress> structuredProgressConsumer;

	private final AtomicBoolean cancelled = new AtomicBoolean(false);

	public Monitor(final Consumer<String> progressMessageConsumer) {
		this(progressMessageConsumer, progress -> {
			// Structured progress is optional.
		});
	}

	public Monitor(final Consumer<String> progressMessageConsumer,
			final Consumer<CrawlProgress> structuredProgressConsumer) {
		this.progressMessageConsumer = Objects.requireNonNull(progressMessageConsumer, "progressMessageConsumer");
		this.structuredProgressConsumer = Objects.requireNonNull(structuredProgressConsumer,
				"structuredProgressConsumer");
	}

	public static Monitor observing(final Consumer<CrawlProgress> structuredProgressConsumer) {
		return new Monitor(message -> {
			// The caller requested structured events only.
		}, structuredProgressConsumer);
	}

	public void report(final CrawlProgress progress) {
		Objects.requireNonNull(progress, "progress");
		if (isCancelled()) {
			return;
		}
		dispatch(progress);
	}

	public void postUpdate(final String message) {
		report(CrawlProgress.indeterminate(CrawlProgress.Phase.CRAWLING, message));
	}

	public void cancel(final String message) {
		if (cancelled.compareAndSet(false, true)) {
			dispatch(CrawlProgress.indeterminate(CrawlProgress.Phase.CANCELLED, message));
		}
	}

	public boolean isCancelled() {
		return cancelled.get();
	}

	public void throwIfCancelled() {
		if (isCancelled()) {
			throw new CrawlCancelledException("Archive crawl was cancelled.");
		}
	}

	public void done(final String message) {
		if (!isCancelled()) {
			dispatch(CrawlProgress.indeterminate(CrawlProgress.Phase.COMPLETE, message));
		}
	}

	private void dispatch(final CrawlProgress progress) {
		structuredProgressConsumer.accept(progress);
		progressMessageConsumer.accept(progress.message());
	}

}
