package com.retrocrawler.model.condition;

/**
 * Observed functional health of an item. A defective item is not necessarily
 * completely inoperative; this vocabulary deliberately preserves the source's
 * level of specificity.
 */
public enum FunctionalCondition {

	WORKING,
	PARTIALLY_DEFECTIVE,
	DEFECTIVE
}
