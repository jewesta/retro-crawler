package com.retrocrawler.core.archive.clues;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import com.retrocrawler.core.util.RetroCrawlerException;

/**
 * A clue finder failed, reported against the archive location it was reading.
 * <p>
 * This is what escapes a crawl when clue finding goes wrong, whatever the
 * finder actually threw: a rejected duplicate, an unreadable clue file, or a
 * finder's own parse failure. The original condition is always the
 * {@linkplain #getCause() cause}, so a caller that cares which one it was can
 * still ask.
 * <p>
 * Context is attached in layers, because no single place knows all of it. A
 * finder may report a {@link ClueLocation}. {@code ArchivePathClueFinder} knows
 * the {@link ClueSource} and wraps whatever the finder threw. The digger knows
 * the archive-relative folder and completes the report with {@link #in(Path)}.
 * Each layer produces a new exception rather than mutating one in flight.
 */
@SuppressWarnings("serial")
public class ClueFindingException extends RetroCrawlerException {

	private final String reason;

	private final transient ClueSource source;

	private final transient ClueLocation location;

	private final transient Path folder;

	/**
	 * Reports a failure a finder detected itself, pointing at the position it
	 * was reading. The framework fills in the source and the folder.
	 */
	public ClueFindingException(final String reason, final ClueLocation location) {
		this(reason, location, null, null, null);
	}

	public ClueFindingException(final String reason, final ClueLocation location, final Throwable cause) {
		this(reason, location, null, null, cause);
	}

	private ClueFindingException(final String reason, final ClueLocation location, final ClueSource source,
			final Path folder, final Throwable cause) {
		super(compose(reason, location, source, folder), cause);
		this.reason = reason;
		this.location = location;
		this.source = source;
		this.folder = folder;
	}

	/** Wraps whatever a finder threw, naming what it was reading. */
	static ClueFindingException from(final ClueSource source, final RuntimeException failure) {
		Objects.requireNonNull(source, "source");
		if (failure instanceof final ClueFindingException reported) {
			return new ClueFindingException(reported.reason, reported.location,
					reported.source == null ? source : reported.source, reported.folder, reported.getCause());
		}
		return new ClueFindingException(failure.getMessage(), null, source, null, failure);
	}

	/** Completes the report with the archive-relative folder being crawled. */
	public ClueFindingException in(final Path archiveRelativeFolder) {
		if (folder != null) {
			return this;
		}
		return new ClueFindingException(reason, location, source, archiveRelativeFolder, getCause());
	}

	/** What was being read, once the framework has named it. */
	public Optional<ClueSource> source() {
		return Optional.ofNullable(source);
	}

	/** Where inside that source, when the finder tracked a position. */
	public Optional<ClueLocation> location() {
		return Optional.ofNullable(location);
	}

	/** The archive-relative folder being crawled, once the digger has named it. */
	public Optional<Path> folder() {
		return Optional.ofNullable(folder);
	}

	/** The failure on its own, without the context this exception adds. */
	public String reason() {
		return reason;
	}

	/**
	 * Renders a compiler-style header — {@code where:line:column: who} — so a
	 * failure reads like any other tool's diagnostic and the archive path can be
	 * pasted straight into a shell.
	 */
	private static String compose(final String reason, final ClueLocation location, final ClueSource source,
			final Path folder) {
		final StringBuilder message = new StringBuilder(where(source, folder));
		if (location != null) {
			message.append(':').append(location.line()).append(':').append(location.column());
		}
		if (!message.isEmpty()) {
			message.append(": ");
		}
		message.append(source == null ? "Clue finding failed" : source.actor()).append('.');
		if (reason != null && !reason.isBlank()) {
			message.append('\n').append(reason);
		}
		if (location != null && location.excerpt() != null) {
			message.append("\n  ").append(location.excerpt()).append("\n  ").append(location.caret());
		}
		return message.toString();
	}

	/**
	 * The archive path the reader should open. A file-content finder names the
	 * file below the crawled folder; every other kind is reported against the
	 * folder itself, whose own name the source would only repeat.
	 */
	private static String where(final ClueSource source, final Path folder) {
		final String base = folder == null ? "" : portable(folder);
		if (source == null || !source.namesAFile()) {
			return base.isEmpty() && source != null && source.name() != null ? source.name() : base;
		}
		return base.isEmpty() ? source.name() : base + "/" + source.name();
	}

	private static String portable(final Path folder) {
		final String separator = folder.getFileSystem().getSeparator();
		final String value = folder.toString();
		final String normalized = "/".equals(separator) ? value : value.replace(separator, "/");
		return normalized.isEmpty() ? "." : normalized;
	}

}
