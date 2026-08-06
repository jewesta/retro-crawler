package com.retrocrawler.core.gear;

import java.util.Objects;

import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.util.RetroCrawlerException;

/**
 * Reports an exception raised while resolving one archive artifact into gear.
 */
@SuppressWarnings("serial")
public final class GearResolutionException extends RetroCrawlerException {

	private final ARI source;

	public GearResolutionException(final ARI source, final RuntimeException cause) {
		super(message(source, cause), Objects.requireNonNull(cause, "cause"));
		this.source = Objects.requireNonNull(source, "source");
	}

	/** The artifact whose clues could not be resolved. */
	public ARI source() {
		return source;
	}

	private static String message(final ARI source, final RuntimeException cause) {
		Objects.requireNonNull(source, "source");
		Objects.requireNonNull(cause, "cause");
		final String reason = cause.getMessage();
		return source + ": Gear resolution failed." + (reason == null || reason.isBlank() ? "" : "\n" + reason);
	}

}
