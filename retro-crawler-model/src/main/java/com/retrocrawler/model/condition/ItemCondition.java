package com.retrocrawler.model.condition;

/**
 * Broad, non-graded condition of an item. The values correspond to the
 * portable Schema.org {@code OfferItemCondition} vocabulary.
 * <p>
 * Absence means that no item condition was observed; the constants have no
 * quality ordering.
 */
public enum ItemCondition {

	NEW,
	USED,
	REFURBISHED,
	DAMAGED
}
