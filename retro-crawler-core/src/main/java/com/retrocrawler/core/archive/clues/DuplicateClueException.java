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
	DuplicateClueException(final Clue previous, final ClueLocation previousLocation, final Clue duplicate,
			final ClueLocation duplicateLocation) {
		super(message(previous, previousLocation, duplicate, duplicateLocation));
	}

	private static String message(final Clue previous, final ClueLocation previousLocation, final Clue duplicate,
			final ClueLocation duplicateLocation) {
		final StringBuilder message = new StringBuilder("Duplicate clue key '").append(duplicate.key())
				.append("'. One artifact may contain only one clue for a key. First values: ").append(previous.value())
				.append(", duplicate values: ").append(duplicate.value()).append(".");
		pointer(previous, previousLocation, duplicate, duplicateLocation)
				.ifPresent(pointer -> message.append('\n').append(pointer));
		return message.toString();
	}

	/**
	 * Draws both observations under one quoted line when they came from the
	 * same line of the same source, and otherwise names each separately. Two
	 * clues claiming one key from two different sources is the case a single
	 * caret cannot express, and it is also the interesting one.
	 */
	private static Optional<String> pointer(final Clue previous, final ClueLocation first, final Clue duplicate,
			final ClueLocation second) {
		if (first != null && first.sharesExcerptWith(second)) {
			return Optional
					.of("  " + first.excerpt() + "\n  " + first.caret() + " first\n  " + second.caret() + " duplicate");
		}

		final StringBuilder pointer = new StringBuilder();
		append(pointer, "first", previous, first);
		if (!pointer.isEmpty() && isKnown(duplicate, second)) {
			pointer.append('\n');
		}
		append(pointer, "duplicate", duplicate, second);
		return pointer.isEmpty() ? Optional.empty() : Optional.of(pointer.toString());
	}

	private static boolean isKnown(final Clue clue, final ClueLocation location) {
		return clue.finder().isPresent() || !clue.sources().isEmpty() || location != null;
	}

	private static void append(final StringBuilder pointer, final String label, final Clue clue,
			final ClueLocation location) {
		if (!isKnown(clue, location)) {
			return;
		}
		pointer.append("  The ").append(label).append(" clue was observed");
		clue.finder().ifPresent(finder -> pointer.append(" by ").append(finder));
		if (!clue.sources().isEmpty()) {
			pointer.append(" from ");
			if (clue.sources().size() == 1) {
				pointer.append(clue.sources().getFirst());
			} else {
				pointer.append(clue.sources());
			}
		}
		if (location != null) {
			pointer.append(clue.finder().isPresent() || !clue.sources().isEmpty() ? ", " : " at ")
					.append(location.describe());
		}
		pointer.append('.');
		if (location != null && location.excerpt() != null) {
			pointer.append("\n    ").append(location.excerpt()).append("\n    ").append(location.caret());
		}
	}

}
