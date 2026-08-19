package com.retrocrawler.core.archive.clues;

import java.util.Objects;

/**
 * What a {@link ClueFinder} was reading when it observed a clue.
 * <p>
 * The framework attaches this without the finder's help. A finder is observed
 * against the candidate folder as a whole. When it lazily inspects exactly one
 * file, the more precise file-content source is retained instead.
 *
 * @param kind
 *            whether the finder examined the folder view generally or one
 *            file's content specifically
 * @param name
 *            the relative file name that was read, or {@code null} when the
 *            source is the folder view as a whole
 * @param finder
 *            the finder that was running
 */
public record ClueSource(ClueSourceKind kind, String name, Class<?> finder) {

	public ClueSource {
		Objects.requireNonNull(kind, "kind");
	}

	public static ClueSource folderView(final ClueFinder finder) {
		return new ClueSource(ClueSourceKind.FOLDER_VIEW, null, type(finder));
	}

	public static ClueSource fileContent(final String fileName, final ClueFinder finder) {
		return new ClueSource(ClueSourceKind.FILE_CONTENT, fileName, type(finder));
	}

	private static Class<?> type(final ClueFinder finder) {
		return finder == null ? null : finder.getClass();
	}

	/**
	 * Renders {@code RetroMarkdownClueFinder read the file content of
	 * 'retro.md'}, for a report that carries no archive path of its own.
	 */
	public String describe() {
		return name == null ? actor() : actor() + " of '" + name + "'";
	}

	/**
	 * Renders what the finder examined without repeating a source name already
	 * present in the archive path.
	 */
	public String actor() {
		final String finderName = finder == null ? "A clue finder" : finder.getSimpleName();
		return finderName + " read the " + kind.label();
	}

	/** Whether {@link #name()} identifies a file below the crawled folder. */
	boolean namesAFile() {
		return kind == ClueSourceKind.FILE_CONTENT && name != null;
	}

	@Override
	public String toString() {
		return describe();
	}

}
