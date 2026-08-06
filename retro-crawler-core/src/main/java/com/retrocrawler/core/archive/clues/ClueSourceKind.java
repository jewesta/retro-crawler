package com.retrocrawler.core.archive.clues;

/** The kind of source a {@link ClueFinder} inspects. */
public enum ClueSourceKind {

	FOLDER_NAME("folder name"),

	FILE_NAME("file names"),

	FILE_CONTENT("file content"),

	FOLDER_TREE("folder tree");

	private final String label;

	ClueSourceKind(final String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

}
