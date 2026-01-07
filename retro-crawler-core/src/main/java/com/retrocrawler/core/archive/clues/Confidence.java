package com.retrocrawler.core.archive.clues;

/**
 * Do not change order, ordinal is used for priority.
 */
public enum Confidence {

	// not a match
	NONE,
	// plausible but ambiguous (e.g. "64")
	WEAK,
	// strong evidence (e.g. "640KB", year in plausible range)
	STRONG,
	// unambiguous / validated (checksum, strict format)
	EXACT;

	public boolean isHigherThan(final Confidence other) {
		return compareTo(other) > 0;
	}

}
