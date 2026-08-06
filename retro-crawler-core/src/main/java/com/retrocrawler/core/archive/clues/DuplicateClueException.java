package com.retrocrawler.core.archive.clues;

import java.util.Optional;

import com.retrocrawler.core.util.RetroCrawlerException;

@SuppressWarnings("serial")
public class DuplicateClueException extends RetroCrawlerException {

	public DuplicateClueException() {
		super();
	}

	public DuplicateClueException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public DuplicateClueException(final String message) {
		super(message);
	}

	public DuplicateClueException(final Throwable cause) {
		super(cause);
	}

	/**
	 * Reports a rejected duplicate against both observations, pointing into the
	 * source wherever the finders tracked a position.
	 */
	DuplicateClueException(final Clue previous, final ClueSighting previousSighting, final Clue duplicate,
			final ClueSighting duplicateSighting) {
		super(message(previous, previousSighting, duplicate, duplicateSighting));
	}

	private static String message(final Clue previous, final ClueSighting previousSighting, final Clue duplicate,
			final ClueSighting duplicateSighting) {
		final StringBuilder message = new StringBuilder("Duplicate clue key '").append(duplicate.key())
				.append("'. One artifact may contain only one clue for a key. First values: ").append(previous.value())
				.append(", duplicate values: ").append(duplicate.value()).append(".");
		pointer(previousSighting, duplicateSighting).ifPresent(pointer -> message.append('\n').append(pointer));
		return message.toString();
	}

	/**
	 * Draws both observations under one quoted line when they came from the
	 * same line of the same source, and otherwise names each separately. Two
	 * clues claiming one key from two different sources is the case a single
	 * caret cannot express, and it is also the interesting one.
	 */
	private static Optional<String> pointer(final ClueSighting previous, final ClueSighting duplicate) {
		final ClueLocation first = previous.location();
		final ClueLocation second = duplicate.location();
		if (first != null && first.sharesExcerptWith(second)) {
			return Optional
					.of("  " + first.excerpt() + "\n  " + first.caret() + " first\n  " + second.caret() + " duplicate");
		}

		final StringBuilder pointer = new StringBuilder();
		append(pointer, "first", previous);
		if (!pointer.isEmpty() && duplicate.isKnown()) {
			pointer.append('\n');
		}
		append(pointer, "duplicate", duplicate);
		return pointer.isEmpty() ? Optional.empty() : Optional.of(pointer.toString());
	}

	private static void append(final StringBuilder pointer, final String label, final ClueSighting sighting) {
		if (!sighting.isKnown()) {
			return;
		}
		pointer.append("  The ").append(label).append(" clue was observed where ").append(sighting.describe())
				.append('.');
		final ClueLocation location = sighting.location();
		if (location != null && location.excerpt() != null) {
			pointer.append("\n    ").append(location.excerpt()).append("\n    ").append(location.caret());
		}
	}

}
