package com.retrocrawler.core.archive.clues;

import java.util.Objects;

/**
 * What a {@link ClueFinder} was reading when it observed a clue.
 * <p>
 * The framework always knows this and attaches it without the finder's help, so
 * even a finder that reports no {@link ClueLocation} produces a failure naming
 * the finder, the kind of source, and the file or folder name it read.
 *
 * @param kind
 *            which of the four kinds of source was inspected
 * @param name
 *            the folder name or file name that was read, or {@code null} when
 *            the source is a whole folder tree
 * @param finder
 *            the finder that was running
 */
public record ClueSource(ClueSourceKind kind, String name, Class<?> finder) {

	public ClueSource {
		Objects.requireNonNull(kind, "kind");
	}

	public static ClueSource folderName(final String folderName, final ClueFinder finder) {
		return new ClueSource(ClueSourceKind.FOLDER_NAME, folderName, type(finder));
	}

	public static ClueSource fileNames(final ClueFinder finder) {
		return new ClueSource(ClueSourceKind.FILE_NAME, null, type(finder));
	}

	public static ClueSource fileContent(final String fileName, final ClueFinder finder) {
		return new ClueSource(ClueSourceKind.FILE_CONTENT, fileName, type(finder));
	}

	public static ClueSource folderTree(final ClueFinder finder) {
		return new ClueSource(ClueSourceKind.FOLDER_TREE, null, type(finder));
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
	 * Renders {@code RetroMarkdownClueFinder read the file content}, without
	 * naming the file. Used where the archive path already names it.
	 */
	public String actor() {
		final String finderName = finder == null ? "A clue finder" : finder.getSimpleName();
		return finderName + " read the " + kind.label();
	}

	/** Whether {@link #name()} identifies a file rather than the crawled folder. */
	boolean namesAFile() {
		return kind == ClueSourceKind.FILE_CONTENT && name != null;
	}

	@Override
	public String toString() {
		return describe();
	}

}
