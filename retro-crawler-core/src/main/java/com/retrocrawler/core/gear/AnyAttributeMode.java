package com.retrocrawler.core.gear;

/**
 * Controls what gets injected into a {@code @RetroAnyAttribute} map.
 */
public enum AnyAttributeMode {

	/**
	 * Only attributes that were not assigned to any declared field (default
	 * behavior).
	 */
	UNASSIGNED_ONLY,

	/**
	 * All facts and all clues, regardless of whether they were assigned to fields.
	 */
	ALL_FACTS_AND_CLUES,

	/**
	 * Only clues. Includes assigned and unassigned clues.
	 */
	CLUES_ONLY,

	/**
	 * Only facts. Includes assigned and unassigned facts.
	 */
	FACTS_ONLY
}