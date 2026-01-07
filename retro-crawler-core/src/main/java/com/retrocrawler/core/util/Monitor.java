package com.retrocrawler.core.util;

import java.util.function.Consumer;

public class Monitor {

	private Consumer<String> progressMessageConsumer;

	private boolean cancelled = false;

	public Monitor(Consumer<String> progressMessageConsumer) {
		super();
		this.progressMessageConsumer = progressMessageConsumer;
	}

	public void postUpdate(String message) {
		if (isCancelled()) {
			return;
		}
		progressMessageConsumer.accept(message);
	}

	public void cancel(String message) {
		this.cancelled = true;
		progressMessageConsumer.accept(message);
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void done(String message) {
		progressMessageConsumer.accept(message);
	}

}
