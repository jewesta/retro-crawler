package com.retrocrawler.core.archive.clues;

import java.util.Objects;

/**
 * A position within one clue source, in the style of a parser diagnostic.
 * <p>
 * Only a {@link ClueFinder} can know this. The framework always knows which
 * folder was crawled, which finder ran, and which file it read, but it cannot
 * know where inside a folder name or a document an observation came from: a
 * finder normalizes what it reads, so the raw text is no longer searchable for
 * the resulting clue. A finder that tracks offsets anyway can hand them over
 * when it accumulates a clue, and a finder that does not simply reports
 * nothing — the surrounding context is reported either way.
 * <p>
 * Lines and columns are one-based so that they read like an editor's.
 *
 * @param line
 *            one-based line within the source
 * @param column
 *            one-based column within that line
 * @param length
 *            how many characters the observation spans, possibly zero
 * @param excerpt
 *            the text of the located line, or {@code null} when the finder
 *            cannot quote it
 */
public record ClueLocation(int line, int column, int length, String excerpt) {

	public ClueLocation {
		if (line < 1) {
			throw new IllegalArgumentException("Expected a one-based line but got: " + line);
		}
		if (column < 1) {
			throw new IllegalArgumentException("Expected a one-based column but got: " + column);
		}
		if (length < 0) {
			throw new IllegalArgumentException("Expected a non-negative length but got: " + length);
		}
	}

	/**
	 * Locates a character range within the complete source text and derives the
	 * line, the column, and the quotable line from it.
	 * <p>
	 * This is the convenient form for a finder that reads a folder name, a file
	 * name, or a whole document into a string and works with plain offsets.
	 */
	public static ClueLocation in(final String text, final int offset, final int length) {
		Objects.requireNonNull(text, "text");
		if (offset < 0 || offset > text.length()) {
			throw new IndexOutOfBoundsException(
					"Offset " + offset + " is outside a source of length " + text.length() + ".");
		}

		int line = 1;
		int lineStart = 0;
		for (int index = 0; index < offset; index++) {
			if (text.charAt(index) == '\n') {
				line++;
				lineStart = index + 1;
			}
		}

		int lineEnd = text.indexOf('\n', lineStart);
		if (lineEnd < 0) {
			lineEnd = text.length();
		}
		if (lineEnd > lineStart && text.charAt(lineEnd - 1) == '\r') {
			lineEnd--;
		}

		return new ClueLocation(line, offset - lineStart + 1, length, text.substring(lineStart, lineEnd));
	}

	/**
	 * Reports a position a finder already tracks in line and column form, for
	 * example while reading a document line by line.
	 */
	public static ClueLocation at(final int line, final int column, final int length, final String excerpt) {
		return new ClueLocation(line, column, length, excerpt);
	}

	/** Reports a whole line without pointing at anything inside it. */
	public static ClueLocation atLine(final int line, final String excerpt) {
		return new ClueLocation(line, 1, excerpt == null ? 0 : excerpt.length(), excerpt);
	}

	/** Renders {@code line 4, column 1}. */
	public String describe() {
		return "line " + line + ", column " + column;
	}

	/**
	 * Whether two locations point into the same quotable line and can therefore
	 * share one excerpt with two carets beneath it.
	 */
	boolean sharesExcerptWith(final ClueLocation other) {
		return other != null && excerpt != null && line == other.line && excerpt.equals(other.excerpt);
	}

	/** A caret run under the located characters, padded to this column. */
	String caret() {
		return " ".repeat(column - 1) + "^".repeat(Math.max(1, length));
	}

}
