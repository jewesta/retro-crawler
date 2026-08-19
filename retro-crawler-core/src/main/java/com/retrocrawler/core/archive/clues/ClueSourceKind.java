package com.retrocrawler.core.archive.clues;

/** The kind of source a {@link ClueFinder} inspects. */
public enum ClueSourceKind {

	FOLDER_VIEW("archive folder"),

	FILE_CONTENT("file content");

	private final String label;

	ClueSourceKind(final String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

}
