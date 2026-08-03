package com.retrocrawler.core.archive.clues;

/**
 * Clue keys reserved for use by RetroCrawler.
 * <p>
 * {@link #ID} and {@link #FOLDER} are synthetic clues added while crawling an
 * archive. {@link #TYPE} is reserved as a serialization discriminator and must
 * not be used as a clue.
 */
public final class InternalClueKeys {

	public static final String PREFIX = "@";

	public static final String ID = PREFIX + "id";

	public static final String FOLDER = PREFIX + "folder";

	public static final String TYPE = PREFIX + "type";

	private InternalClueKeys() {
		// static constants
	}

}
