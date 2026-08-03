package com.retrocrawler.model.packaging;

/**
 * Explicitly observed state of a package seal.
 * <p>
 * Absence of a value means that the seal state is unknown. The values are
 * mutually exclusive but do not imply item condition, completeness, or a
 * particular packaging form.
 */
public enum SealState {

	SEALED,

	OPENED
}
