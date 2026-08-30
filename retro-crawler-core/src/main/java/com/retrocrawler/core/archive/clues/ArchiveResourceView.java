package com.retrocrawler.core.archive.clues;

import java.util.Set;

import com.retrocrawler.core.archive.ARI;

/** A read-only crawl-time view of one authoritatively identified resource. */
public interface ArchiveResourceView {

	/** The stable identity assigned by the active archive definition. */
	ARI ari();

	/** Creates an anonymous clue sourced from this resource. */
	default Clue clue(final String value) {
		return Clue.of(value).from(ari());
	}

	/** Creates a keyed clue sourced from this resource. */
	default Clue clue(final String key, final String value) {
		return Clue.of(key, value).from(ari());
	}

	/** Creates an anonymous multi-value clue sourced from this resource. */
	default Clue clue(final Set<String> values) {
		return Clue.of(values).from(ari());
	}

	/** Creates a keyed multi-value clue sourced from this resource. */
	default Clue clue(final String key, final Set<String> values) {
		return Clue.of(key, values).from(ari());
	}

	/** Creates a missing-value clue sourced from this resource. */
	default Clue missingValue(final String key) {
		return Clue.missingValue(key).from(ari());
	}
}
