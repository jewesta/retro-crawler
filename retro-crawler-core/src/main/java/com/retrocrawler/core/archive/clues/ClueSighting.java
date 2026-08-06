package com.retrocrawler.core.archive.clues;

/**
 * Where one accumulated clue was spotted: what was being read, and optionally
 * where inside it.
 * <p>
 * A {@link ClueAccumulator} remembers a sighting per accepted clue so that a
 * duplicate can be reported against both observations rather than only the
 * second one. The position travels with {@link Clues} so that it survives a
 * finder handing its work back, and stops at the {@link Artifact} boundary,
 * which is where a cached archive can no longer point into anything. The
 * source is never carried: it belongs to the finder currently running.
 *
 * @param source
 *            what was read, or {@code null} when a finder accumulates on its
 *            own and the framework has not named the source yet
 * @param location
 *            where inside the source, or {@code null} when the finder tracks no
 *            offsets
 */
record ClueSighting(ClueSource source, ClueLocation location) {

	static final ClueSighting UNKNOWN = new ClueSighting(null, null);

	boolean isKnown() {
		return source != null || location != null;
	}

	/** Renders {@code BracketClueFinder read the folder name, line 1, column 15}. */
	String describe() {
		if (source == null) {
			return location == null ? "an unreported position" : location.describe();
		}
		return location == null ? source.describe() : source.describe() + ", " + location.describe();
	}

}
