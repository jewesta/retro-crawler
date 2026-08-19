package com.retrocrawler.core.archive.clues;

import java.util.Objects;
import java.util.Optional;

import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.util.RetroCrawlerException;

/**
 * A clue-finding failure reported against an authoritatively identified
 * resource. The original failure remains available as the cause.
 */
@SuppressWarnings("serial")
public class ClueFindingException extends RetroCrawlerException {

	private final String reason;

	private final String finder;

	private final transient ARI source;

	private final transient ClueLocation location;

	/**
	 * Reports a failure a finder detected itself, pointing at the position it
	 * was reading. The framework fills in the finder and resource identity.
	 */
	public ClueFindingException(final String reason, final ClueLocation location) {
		this(reason, null, null, location, null);
	}

	public ClueFindingException(final String reason, final ClueLocation location, final Throwable cause) {
		this(reason, null, null, location, cause);
	}

	private ClueFindingException(final String reason, final String finder, final ARI source,
			final ClueLocation location, final Throwable cause) {
		super(compose(reason, finder, source, location), cause);
		this.reason = reason;
		this.finder = finder;
		this.source = source;
		this.location = location;
	}

	/** Wraps whatever a finder threw, adding its name and default resource. */
	public static ClueFindingException from(final String finder, final ARI source, final RuntimeException failure) {
		Objects.requireNonNull(source, "source");
		Objects.requireNonNull(failure, "failure");
		if (failure instanceof final ClueFindingException reported) {
			final Throwable cause = reported.getCause() == null ? reported : reported.getCause();
			return new ClueFindingException(reported.reason, reported.finder == null ? finder : reported.finder,
					reported.source == null ? source : reported.source, reported.location, cause);
		}
		return new ClueFindingException(failure.getMessage(), finder, source, null, failure);
	}

	/**
	 * Adds an exact resource to a failure raised while that resource was open.
	 */
	public static ClueFindingException at(final ARI source, final RuntimeException failure) {
		return from(null, source, failure);
	}

	/** The configured finder name, once the framework has attached it. */
	public Optional<String> finder() {
		return Optional.ofNullable(finder);
	}

	/**
	 * The exact resource being inspected, or the candidate folder by default.
	 */
	public Optional<ARI> source() {
		return Optional.ofNullable(source);
	}

	/** The source archive, once a resource has been attached. */
	public Optional<ArchiveId> archiveId() {
		return source().map(ARI::archiveId);
	}

	/** Where inside that source, when the finder tracked a position. */
	public Optional<ClueLocation> location() {
		return Optional.ofNullable(location);
	}

	/** The failure on its own, without the context this exception adds. */
	public String reason() {
		return reason;
	}

	private static String compose(final String reason, final String finder, final ARI source,
			final ClueLocation location) {
		final StringBuilder message = new StringBuilder();
		if (source != null) {
			message.append(source);
		}
		if (location != null) {
			if (!message.isEmpty()) {
				message.append(':');
			}
			message.append(location.line()).append(':').append(location.column());
		}
		if (!message.isEmpty()) {
			message.append(": ");
		}
		message.append(finder == null ? "Clue finding failed." : finder + " failed while finding clues.");
		if (reason != null && !reason.isBlank()) {
			message.append('\n').append(reason);
		}
		if (location != null && location.excerpt() != null) {
			message.append("\n  ").append(location.excerpt()).append("\n  ").append(location.caret());
		}
		return message.toString();
	}
}
